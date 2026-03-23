package com.broadnet.billing.service;

import com.broadnet.billing.dto.response.*;

/**
 * BILLING CHECKOUT SERVICE
 * Handles Stripe checkout session creation for new subscriptions
 */
public interface BillingCheckoutService {

    /**
     * Create Stripe checkout session
     *
     * Validates:
     * - plan_code exists and is_active = true
     * - billing_interval is valid (month/year)
     *
     * Returns:
     * - checkout_session_id from Stripe
     * - checkout URL for user to complete payment
     */
    CheckoutSessionResponse createCheckoutSession(
            Long userId, String planCode,
            String billingInterval,
            String successUrl,
            String cancelUrl,
            Long companyId
    );
}

