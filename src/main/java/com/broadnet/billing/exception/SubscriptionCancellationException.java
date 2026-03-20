package com.broadnet.billing.exception;

/**
 * Thrown when subscription cannot be canceled
 * HTTP 400
 */
public class SubscriptionCancellationException extends BillingException {
    public SubscriptionCancellationException(String message) {
        super(message, 400, "SUBSCRIPTION_CANCELLATION_FAILED");
    }
}
