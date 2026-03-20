package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.request.UpdateAddressRequest;
import com.broadnet.billing.dto.request.UpdatePaymentMethodRequest;
import com.broadnet.billing.dto.response.BillingDetailsResponse;
import com.broadnet.billing.dto.response.BillingPortalResponse;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.BillingDetailsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.model.billingportal.Session;
import com.stripe.param.CustomerUpdateParams;
import com.stripe.param.PaymentMethodAttachParams;
import com.stripe.param.billingportal.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * ============================================================================
 * BILLING DETAILS SERVICE IMPLEMENTATION
 * Manages billing address, payment methods, and Stripe portal
 * Architecture: Manage payment methods and address, integrate with Stripe
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BillingDetailsServiceImpl implements BillingDetailsService {

    private final CompanyBillingRepository billingRepository;
    private final BillingPaymentMethodRepository paymentMethodRepository;
    private final ObjectMapper objectMapper;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${app.billing.portal.return.url:https://app.broadnet.ai/billing}")
    private String billingPortalReturnUrl;

    /**
     * Get current billing details
     *
     * Fetches:
     * - Billing address from BillingPaymentMethod
     * - Default payment method (non-expired)
     * - Company name and tax ID
     */
    @Override
    @Transactional(readOnly = true)
    public BillingDetailsResponse getBillingDetails(Long companyId) {

        log.debug("Getting billing details for company: {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        BillingPaymentMethod paymentMethod = paymentMethodRepository
                .findDefaultByCompanyId(companyId)
                .orElse(null);

        BillingDetailsResponse.AddressDto address = null;
        BillingDetailsResponse.PaymentMethodDetailsDto paymentDetails = null;

        if (paymentMethod != null) {
            // Parse billing details JSON
            address = parseBillingAddress(paymentMethod.getBillingDetails());

            paymentDetails = BillingDetailsResponse.PaymentMethodDetailsDto.builder()
                    .type(paymentMethod.getType().name())
                    .brand(paymentMethod.getCardBrand())
                    .last4(paymentMethod.getCardLast4())
                    .expMonth(paymentMethod.getCardExpMonth())
                    .expYear(paymentMethod.getCardExpYear())
                    .isDefault(paymentMethod.getIsDefault())
                    .isExpired(paymentMethod.getIsExpired())
                    .build();
        }

        return BillingDetailsResponse.builder()
                .billingAddress(address)
                .paymentMethod(paymentDetails)
//                .taxId(billing.getTaxId())
//                .companyName(billing.getCompanyName())
                .build();
    }

    /**
     * Update billing address
     *
     * Validates:
     * - All required fields present
     * - Valid country code
     *
     * Updates:
     * - BillingPaymentMethod address fields
     * - Syncs with Stripe
     */
    @Override
    @Transactional
    public BillingDetailsResponse updateBillingAddress(
            Long companyId,
            UpdateAddressRequest request) {

        log.info("Updating billing address for company: {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        BillingPaymentMethod paymentMethod = paymentMethodRepository
                .findDefaultByCompanyId(companyId)
                .orElseThrow(() -> new PaymentMethodNotFoundException(companyId));

        // Validate address
        validateAddress(request);

        // Update billing details JSON
        Map<String, String> billingDetailsMap = new HashMap<>();
        billingDetailsMap.put("street", request.getStreet());
        billingDetailsMap.put("city", request.getCity());
        billingDetailsMap.put("state", request.getState());
        billingDetailsMap.put("postalCode", request.getPostalCode());
        billingDetailsMap.put("country", request.getCountry());

        try {
            String billingDetailsJson = objectMapper.writeValueAsString(billingDetailsMap);
            paymentMethod.setBillingDetails(billingDetailsJson);
            paymentMethodRepository.save(paymentMethod);
        } catch (JsonProcessingException e) {
            log.error("Error serializing billing details: {}", e.getMessage());
            throw new RuntimeException("Failed to update billing details", e);
        }

        // Update company name if provided
//        if (request.getCompanyName() != null && !request.getCompanyName().isEmpty()) {
//            billing.setCompanyName(request.getCompanyName());
//            billingRepository.save(billing);
//        }

        // Sync with Stripe
        syncAddressWithStripe(billing, request);

        log.info("Billing address updated for company: {}", companyId);

        return getBillingDetails(companyId);
    }

    /**
     * Update default payment method
     *
     * Validates:
     * - Stripe token is valid
     *
     * Operations:
     * 1. Attach payment method to Stripe customer
     * 2. Create BillingPaymentMethod entity
     * 3. If set_as_default: Update previous default
     * 4. Sync with Stripe
     */
    @Override
    @Transactional
    public BillingDetailsResponse updatePaymentMethod(
            Long companyId,
            UpdatePaymentMethodRequest request) {

        log.info("Updating payment method for company: {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        try {
            Stripe.apiKey = stripeApiKey;

            // Attach payment method to customer
            PaymentMethod paymentMethod = PaymentMethod.retrieve(request.getPaymentMethodToken());
            paymentMethod.attach(PaymentMethodAttachParams.builder()
                    .setCustomer(billing.getStripeCustomerId())
                    .build());

            // Create or update BillingPaymentMethod
            BillingPaymentMethod billingPaymentMethod = new BillingPaymentMethod();
            billingPaymentMethod.setCompanyId(companyId);
            billingPaymentMethod.setStripePaymentMethodId(paymentMethod.getId());
            billingPaymentMethod.setType(BillingPaymentMethod.PaymentMethodType.valueOf(paymentMethod.getType()));

            // Set card details if card type
            if ("card".equals(paymentMethod.getType())) {
                PaymentMethod.Card card = paymentMethod.getCard();
                if (card != null) {
                    billingPaymentMethod.setCardBrand(card.getBrand());
                    billingPaymentMethod.setCardLast4(card.getLast4());
                    billingPaymentMethod.setCardExpMonth(card.getExpMonth().intValue());
                    billingPaymentMethod.setCardExpYear(card.getExpYear().intValue());
                }
            }

            // Set as default if requested
            if (request.isSetAsDefault()) {
                // Unset previous default
                paymentMethodRepository.findDefaultByCompanyId(companyId)
                        .ifPresent(pm -> {
                            pm.setIsDefault(false);
                            paymentMethodRepository.save(pm);
                        });

                billingPaymentMethod.setIsDefault(true);

                // Update Stripe customer default payment method
                Customer customer = Customer.retrieve(billing.getStripeCustomerId());
                customer.update(CustomerUpdateParams.builder()
                        .setInvoiceSettings(CustomerUpdateParams.InvoiceSettings.builder()
                                .setDefaultPaymentMethod(paymentMethod.getId())
                                .build())
                        .build());
            }

            paymentMethodRepository.save(billingPaymentMethod);

            log.info("Payment method updated for company: {}", companyId);
            return getBillingDetails(companyId);

        } catch (StripeException e) {
            log.error("Stripe error updating payment method: {}", e.getMessage());
            throw new StripeApiException(
                    "Failed to update payment method: " + e.getMessage(),
                    e.getCode()
            );
        }
    }

    /**
     * Get Stripe customer portal URL
     *
     * User can manage payment methods, download invoices in portal
     */
    @Override
    @Transactional(readOnly = true)
    public BillingPortalResponse getStripePortalUrl(Long companyId) {

        log.debug("Generating Stripe portal URL for company: {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        if (billing.getStripeCustomerId() == null) {
            throw new IllegalStateException("No Stripe customer found for company: " + companyId);
        }

        try {
            Stripe.apiKey = stripeApiKey;

            Session session = Session.create(
                    SessionCreateParams.builder()
                            .setCustomer(billing.getStripeCustomerId())
                            .setReturnUrl(billingPortalReturnUrl)
                            .build()
            );

            return BillingPortalResponse.builder()
                    .url(session.getUrl())
                    .build();

        } catch (StripeException e) {
            log.error("Stripe error generating portal URL: {}", e.getMessage());
            throw new StripeApiException(
                    "Failed to generate portal URL: " + e.getMessage(),
                    e.getCode()
            );
        }
    }

    // ===== Helper Methods =====

    /**
     * Validate address fields
     */
    private void validateAddress(UpdateAddressRequest request) {
        if (request.getStreet() == null || request.getStreet().isEmpty()) {
            throw new InvalidAddressException("Street is required");
        }
        if (request.getCity() == null || request.getCity().isEmpty()) {
            throw new InvalidAddressException("City is required");
        }
        if (request.getState() == null || request.getState().isEmpty()) {
            throw new InvalidAddressException("State is required");
        }
        if (request.getPostalCode() == null || request.getPostalCode().isEmpty()) {
            throw new InvalidAddressException("Postal code is required");
        }
        if (request.getCountry() == null || request.getCountry().length() != 2) {
            throw new InvalidAddressException("Country must be 2-letter ISO code");
        }
    }

    /**
     * Parse billing address from JSON
     */
    private BillingDetailsResponse.AddressDto parseBillingAddress(String billingDetailsJson) {
        if (billingDetailsJson == null || billingDetailsJson.isEmpty()) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> detailsMap = objectMapper.readValue(billingDetailsJson, Map.class);

            return BillingDetailsResponse.AddressDto.builder()
                    .street(detailsMap.get("street"))
                    .city(detailsMap.get("city"))
                    .state(detailsMap.get("state"))
                    .postalCode(detailsMap.get("postalCode"))
                    .country(detailsMap.get("country"))
                    .build();
        } catch (JsonProcessingException e) {
            log.error("Error parsing billing details JSON: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Sync address with Stripe
     */
    private void syncAddressWithStripe(CompanyBilling billing, UpdateAddressRequest request) {
        try {
            Stripe.apiKey = stripeApiKey;

            Customer customer = Customer.retrieve(billing.getStripeCustomerId());
            customer.update(CustomerUpdateParams.builder()
                    .setAddress(CustomerUpdateParams.Address.builder()
                            .setLine1(request.getStreet())
                            .setCity(request.getCity())
                            .setState(request.getState())
                            .setPostalCode(request.getPostalCode())
                            .setCountry(request.getCountry())
                            .build())
                    .build());

        } catch (StripeException e) {
            log.warn("Failed to sync address with Stripe: {}", e.getMessage());
            // Don't throw - continue even if Stripe sync fails
        }
    }
}