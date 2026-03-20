package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.CreateCheckoutSessionRequest;
import com.broadnet.billing.dto.response.CheckoutSessionResponse;
import com.broadnet.billing.service.BillingCheckoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Checkout API Controller
 * Handles Stripe checkout session creation for new subscriptions
 *
 * Security: Requires authenticated user
 * Rate Limiting: 10 requests/minute per user
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/checkout")
@RequiredArgsConstructor
public class BillingCheckoutController {

    private final BillingCheckoutService checkoutService;

    /**
     * Create Stripe checkout session for subscription
     *
     * POST /api/billing/checkout/create-session
     *
     * @param request CreateCheckoutSessionRequest with plan_code, billing_interval, success/cancel URLs
     * @return CheckoutSessionResponse with checkout_session_id and url
     *
     * Flow:
     * 1. Validate plan_code exists and is active
     * 2. Get BillingStripePrice for plan + billing_interval
     * 3. Create Stripe checkout session
     * 4. Store session reference (optional)
     * 5. Return session URL
     *
     * Error Cases:
     * - 400: Invalid plan_code or billing_interval
     * - 400: Plan not found or inactive
     * - 500: Stripe API error
     */
    @PostMapping("/create-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @Valid @RequestBody CreateCheckoutSessionRequest request,
            @RequestHeader(value = "Authorization") String authToken) {

        log.info("Creating checkout session for plan: {}, interval: {}",
                request.getPlanCode(), request.getBillingInterval());

        try {
            CheckoutSessionResponse response = checkoutService.createCheckoutSession(
                    request.getPlanCode(),
                    request.getBillingInterval(),
                    request.getSuccessUrl(),
                    request.getCancelUrl(),
                    Long.valueOf(authToken)  // Extract company_id from token
            );

            log.info("Checkout session created: {}", response.getCheckoutSessionId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid checkout request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error creating checkout session", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}