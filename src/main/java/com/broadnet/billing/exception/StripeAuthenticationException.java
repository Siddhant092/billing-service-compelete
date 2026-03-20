package com.broadnet.billing.exception;

/**
 * Thrown when Stripe authentication fails
 * HTTP 401
 */
public class StripeAuthenticationException extends BillingException {
    public StripeAuthenticationException(String message) {
        super(message, 401, "STRIPE_AUTH_FAILED");
    }
}
