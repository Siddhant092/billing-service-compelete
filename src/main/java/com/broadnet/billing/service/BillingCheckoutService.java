package com.broadnet.billing.service;

import com.broadnet.billing.dto.request.*;
import com.broadnet.billing.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
            String planCode,
            String billingInterval,
            String successUrl,
            String cancelUrl,
            Long companyId
    );
}

