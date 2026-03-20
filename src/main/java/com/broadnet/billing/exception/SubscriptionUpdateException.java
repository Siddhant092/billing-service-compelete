package com.broadnet.billing.exception;

/**
 * Thrown when subscription update fails
 * HTTP 502
 */
public class SubscriptionUpdateException extends BillingException {
    public SubscriptionUpdateException(String message) {
        super(message, 502, "SUBSCRIPTION_UPDATE_FAILED");
    }

    public SubscriptionUpdateException(String message, Throwable cause) {
        super(message, cause, 502, "SUBSCRIPTION_UPDATE_FAILED");
    }
}
