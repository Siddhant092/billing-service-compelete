package com.broadnet.billing.exception;

/**
 * Thrown when subscription cannot be reactivated
 * HTTP 400
 */
public class SubscriptionReactivationException extends BillingException {
    public SubscriptionReactivationException(String message) {
        super(message, 400, "SUBSCRIPTION_REACTIVATION_FAILED");
    }
}
