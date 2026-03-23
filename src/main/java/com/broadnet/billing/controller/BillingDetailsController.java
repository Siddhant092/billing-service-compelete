package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.UpdateAddressRequest;
import com.broadnet.billing.dto.request.UpdatePaymentMethodRequest;
import com.broadnet.billing.dto.response.BillingDetailsResponse;
import com.broadnet.billing.dto.response.BillingPortalResponse;
import com.broadnet.billing.service.BillingDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Billing Details API Controller
 * Manages billing address, payment methods, and Stripe portal access
 *
 * Security: Requires authenticated user
 * Rate Limiting: 10 requests/minute
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/details")
@RequiredArgsConstructor
public class BillingDetailsController {

    private final BillingDetailsService detailsService;

    /**
     * Get current billing details
     *
     * GET /api/billing/details
     *
     * @return BillingDetailsResponse with address and payment method
     *
     * Response:
     * {
     *   "billing_address": {
     *     "street": "123 Main St",
     *     "city": "San Francisco",
     *     "state": "CA",
     *     "postal_code": "94105",
     *     "country": "US"
     *   },
     *   "payment_method": {
     *     "type": "card",
     *     "brand": "visa",
     *     "last4": "4242",
     *     "exp_month": 12,
     *     "exp_year": 2026,
     *     "is_default": true,
     *     "is_expired": false
     *   },
     *   "tax_id": null,
     *   "company_name": "Acme Corp"
     * }
     *
     * Filter:
     * - Current default payment method only
     * - Non-expired address
     *
     * Caching: 5min TTL
     */
    @GetMapping
    public ResponseEntity<BillingDetailsResponse> getBillingDetails(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching billing details for company: {}", companyId);

        try {
            BillingDetailsResponse details = detailsService.getBillingDetails(userId, companyId);
            return ResponseEntity.ok(details);

        } catch (Exception e) {
            log.error("Error fetching billing details for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update billing address
     *
     * PUT /api/billing/details/address
     *
     * @param request UpdateAddressRequest with street, city, state, postal_code, country
     * @return Updated BillingDetailsResponse
     *
     * Request:
     * {
     *   "street": "456 Oak Ave",
     *   "city": "New York",
     *   "state": "NY",
     *   "postal_code": "10001",
     *   "country": "US",
     *   "company_name": "Acme Corp Inc"
     * }
     *
     * Flow:
     * 1. Validate address (required fields)
     * 2. Get BillingPaymentMethod (default)
     * 3. Update billing details on BillingPaymentMethod entity
     * 4. Sync with Stripe (customer.address)
     * 5. Create audit log
     *
     * Error Cases:
     * - 400: Invalid address
     * - 401: Unauthorized
     * - 500: Stripe error
     */
    @PutMapping("/address")
    public ResponseEntity<BillingDetailsResponse> updateBillingAddress(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateAddressRequest request,
            @RequestParam(value = "company_id") Long companyId) {

        log.info("Updating billing address for company: {}", companyId);

        try {
            BillingDetailsResponse details = detailsService.updateBillingAddress(
                    userId,
                    companyId,
                    request
            );

            log.info("Billing address updated for company: {}", companyId);
            return ResponseEntity.ok(details);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid address update for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating billing address for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update default payment method
     * Creates new payment method and sets as default
     *
     * POST /api/billing/payment-methods/update
     *
     * @param request Payment method details (Stripe token)
     * @return Updated payment method
     *
     * Request:
     * {
     *   "payment_method_token": "pm_...",  // Stripe PaymentMethod ID
     *   "set_as_default": true
     * }
     *
     * Flow:
     * 1. Validate Stripe token
     * 2. Attach payment method to Stripe customer
     * 3. Create BillingPaymentMethod entity
     * 4. If set_as_default: Update previous default, mark new as default
     * 5. Sync with Stripe customer
     *
     * Error Cases:
     * - 400: Invalid token
     * - 401: Unauthorized
     * - 402: Payment required (Stripe declined)
     * - 500: Stripe error
     */
    @PostMapping("/payment-methods/update")
    public ResponseEntity<BillingDetailsResponse> updatePaymentMethod(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdatePaymentMethodRequest request,
            @RequestParam(value = "company_id") Long companyId) {

        log.info("Updating payment method for company: {}", companyId);

        try {
            BillingDetailsResponse details = detailsService.updatePaymentMethod(
                    userId,
                    companyId,
                    request
            );

            log.info("Payment method updated for company: {}", companyId);
            return ResponseEntity.ok(details);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid payment method update for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating payment method for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get Stripe customer portal URL
     * User can manage payment methods, invoices in Stripe portal
     *
     * POST /api/billing/stripe-portal
     *
     * @return BillingPortalResponse with portal URL
     *
     * Response:
     * {
     *   "url": "https://billing.stripe.com/session/..."
     * }
     *
     * Flow:
     * 1. Get Stripe customer ID
     * 2. Create billing portal session
     * 3. Return portal URL
     * 4. User opens URL in browser
     *
     * Note: This is a convenience endpoint - users can also go to Stripe portal directly
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 404: No Stripe customer
     * - 500: Stripe error
     */
    @PostMapping("/stripe-portal")
    public ResponseEntity<BillingPortalResponse> getStripePortalUrl(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Generating Stripe portal URL for company: {}", companyId);

        try {
            BillingPortalResponse response = detailsService.getStripePortalUrl(userId, companyId);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("No Stripe customer for company: {}", companyId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error generating Stripe portal URL for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}