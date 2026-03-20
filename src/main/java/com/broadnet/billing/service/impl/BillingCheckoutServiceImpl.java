package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.response.CheckoutSessionResponse;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.BillingCheckoutService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * ============================================================================
 * BILLING CHECKOUT SERVICE IMPLEMENTATION
 * Handles Stripe checkout session creation for subscriptions
 * Architecture: Stripe-First, creates checkout and stores reference in DB
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BillingCheckoutServiceImpl implements BillingCheckoutService {

    // ===== Dependencies =====
    private final BillingPlanRepository planRepository;
    private final BillingStripePriceRepository priceRepository;
    private final CompanyBillingRepository billingRepository;
    private final BillingWebhookEventRepository webhookEventRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.webhook.url:https://api.broadnet.ai}")
    private String webhookBaseUrl;

    // ===== Service Methods =====

    /**
     * Create Stripe checkout session for subscription
     *
     * Flow:
     * 1. Validate plan exists and is active
     * 2. Get BillingStripePrice for plan + billing_interval
     * 3. Get CompanyBilling (create if not exists)
     * 4. Create Stripe checkout session with price
     * 5. Return session URL
     *
     * @param planCode Plan code (e.g., "professional")
     * @param billingInterval "month" or "year"
     * @param successUrl URL after successful payment
     * @param cancelUrl URL if user cancels
     * @param companyId Company ID for this subscription
     * @return CheckoutSessionResponse with session ID and URL
     */
    @Override
    @Transactional
    public CheckoutSessionResponse createCheckoutSession(
            String planCode,
            String billingInterval,
            String successUrl,
            String cancelUrl,
            Long companyId) {

        log.info("Creating checkout session - Plan: {}, Interval: {}, Company: {}",
                planCode, billingInterval, companyId);

        // Step 1: Validate plan exists and is active
        BillingPlan plan = planRepository.findByPlanCode(planCode)
                .orElseThrow(() -> new BillingPlanNotFoundException(planCode));

        if (!plan.getIsActive()) {
            throw new InactivePlanException(planCode);
        }

        // Step 2: Validate billing interval and convert to enum
        if (!isValidBillingInterval(billingInterval)) {
            throw new InvalidBillingOperationException(
                    "Invalid billing interval: " + billingInterval + ". Must be 'month' or 'year'");
        }

        // Convert String to Enum
        BillingStripePrice.BillingInterval intervalEnum = BillingStripePrice.BillingInterval.valueOf(
                billingInterval.toLowerCase()
        );

        // Step 3: Get Stripe price for this plan + interval
        BillingStripePrice stripePrice = priceRepository
                .findActivePriceByPlanAndInterval(plan.getId(), intervalEnum)
                .orElseThrow(() -> new StripeObjectNotFoundException(
                        String.format("No active price found for plan %s interval %s", planCode, billingInterval)));

        // Step 4: Get or create CompanyBilling
        CompanyBilling billing = getOrCreateCompanyBilling(companyId, plan);

        // Step 5: Create Stripe checkout session
        try {
            Stripe.apiKey = stripeApiKey;

            Session session = Session.create(
                    buildCheckoutSessionParams(
                            companyId,
                            stripePrice,
                            plan,
                            billingInterval,
                            successUrl,
                            cancelUrl,
                            billing
                    )
            );

            log.info("Checkout session created successfully: {} for company: {}",
                    session.getId(), companyId);

            // Step 6: Return response with session URL
            return CheckoutSessionResponse.builder()
                    .checkoutSessionId(session.getId())
                    .url(session.getUrl())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe API error creating checkout session: {}", e.getMessage());
            throw new CheckoutSessionCreationException(
                    "Failed to create checkout session: " + e.getMessage(), e);
        }
    }

    // ===== Helper Methods =====

    /**
     * Build Stripe checkout session parameters
     *
     * TWO MODES AVAILABLE:
     * 1. HOSTED MODE (default): Uses success_url and cancel_url, redirects to Stripe-hosted page
     * 2. EMBEDDED MODE: Uses return_url only, embeds checkout in your frontend
     *
     * This implementation uses HOSTED MODE for simplicity
     */
    private SessionCreateParams buildCheckoutSessionParams(
            Long companyId,
            BillingStripePrice stripePrice,
            BillingPlan plan,
            String billingInterval,
            String successUrl,
            String cancelUrl,
            CompanyBilling billing) {

        return SessionCreateParams.builder()
                // Mode and customer
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(billing.getStripeCustomerId())

                // HOSTED MODE: Use success_url and cancel_url
                .setSuccessUrl(appendQueryParam(successUrl, "session_id", "{CHECKOUT_SESSION_ID}"))
                .setCancelUrl(cancelUrl)

                // Line items (the subscription plan)
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(stripePrice.getStripePriceId())
                                .setQuantity(1L)
                                .build()
                )

                // Subscription data
                .setSubscriptionData(
                        SessionCreateParams.SubscriptionData.builder()
                                .putMetadata("company_id", companyId.toString())
                                .putMetadata("plan_code", plan.getPlanCode())
                                .putMetadata("billing_interval", billingInterval)
//                                .addAllTaxRate(getTaxRates())  // Add tax rates if applicable
                                .build()
                )

                // NOTE: Invoice creation is AUTOMATIC for subscription mode
                // Stripe automatically creates invoices - do NOT manually enable it

                // Automatic tax calculation (disabled for development - enable in production after configuring Stripe Tax settings)
                // To enable: Add business address at https://dashboard.stripe.com/settings/tax
                // .setAutomaticTax(
                //         SessionCreateParams.AutomaticTax.builder()
                //                 .setEnabled(true)
                //                 .build()
                // )

                .build();
    }

    /**
     * Get or create CompanyBilling record
     * If existing record has invalid Stripe customer ID (like 'temporary1'), create new customer
     */
    private CompanyBilling getOrCreateCompanyBilling(Long companyId, BillingPlan plan) {
        Optional<CompanyBilling> existing = billingRepository.findByCompanyId(companyId);

        if (existing.isPresent()) {
            CompanyBilling billing = existing.get();

            // Check if Stripe customer ID is valid (not a placeholder)
            String customerId = billing.getStripeCustomerId();
            if (customerId != null && !isPlaceholderCustomerId(customerId)) {
                // Valid customer ID exists
                return billing;
            }

            // Invalid/placeholder customer ID - create real Stripe customer
            log.warn("Company {} has placeholder Stripe customer ID '{}', creating real customer",
                    companyId, customerId);

            try {
                Stripe.apiKey = stripeApiKey;
                com.stripe.model.Customer customer = com.stripe.model.Customer.create(
                        new com.stripe.param.CustomerCreateParams.Builder()
                                .putMetadata("company_id", companyId.toString())
                                .build()
                );
                billing.setStripeCustomerId(customer.getId());
                return billingRepository.save(billing);
            } catch (StripeException e) {
                log.error("Failed to create Stripe customer for company {}: {}", companyId, e.getMessage());
                throw new StripeApiException(
                        "Failed to create Stripe customer: " + e.getMessage(),
                        e.getCode()
                );
            }
        }

        // Create new CompanyBilling
        CompanyBilling billing = new CompanyBilling();
        billing.setCompanyId(companyId);
        billing.setActivePlanCode(plan.getPlanCode());
        billing.setActivePlan(plan);
        billing.setBillingMode(CompanyBilling.BillingMode.prepaid);  // Default to prepaid
        billing.setSubscriptionStatus(CompanyBilling.SubscriptionStatus.active);

        // Create Stripe customer
        try {
            Stripe.apiKey = stripeApiKey;
            com.stripe.model.Customer customer = com.stripe.model.Customer.create(
                    new com.stripe.param.CustomerCreateParams.Builder()
                            .putMetadata("company_id", companyId.toString())
                            .build()
            );
            billing.setStripeCustomerId(customer.getId());
        } catch (StripeException e) {
            log.error("Failed to create Stripe customer for company {}: {}", companyId, e.getMessage());
            throw new StripeApiException(
                    "Failed to create Stripe customer: " + e.getMessage(),
                    e.getCode()
            );
        }

        return billingRepository.save(billing);
    }

    /**
     * Check if customer ID is a placeholder/test value
     */
    private boolean isPlaceholderCustomerId(String customerId) {
        if (customerId == null) {
            return true;
        }
        // Check for common placeholder patterns
        return customerId.startsWith("temporary")
                || customerId.startsWith("test_")
                || customerId.startsWith("placeholder_")
                || customerId.equals("temp")
                || customerId.equals("dummy")
                || !customerId.startsWith("cus_");  // Real Stripe customer IDs start with "cus_"
    }

    /**
     * Validate billing interval format
     */
    private boolean isValidBillingInterval(String interval) {
        return interval != null && (interval.equalsIgnoreCase("month") || interval.equalsIgnoreCase("year"));
    }

    /**
     * Get tax rates for region (can be extended)
     */
    private List<String> getTaxRates() {
        // TODO: Implement based on customer location
        return Collections.emptyList();
    }

    /**
     * Append query parameter to URL
     */
    private String appendQueryParam(String url, String paramName, String paramValue) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + paramName + "=" + paramValue;
    }
}