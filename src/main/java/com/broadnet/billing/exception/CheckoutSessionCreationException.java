package com.broadnet.billing.exception;

/**
 * Thrown when checkout session creation fails
 * HTTP 502
 */
public class CheckoutSessionCreationException extends BillingException {
    public CheckoutSessionCreationException(String message) {
        super(message, 502, "CHECKOUT_SESSION_FAILED");
    }

    public CheckoutSessionCreationException(String message, Throwable cause) {
        super(message, cause, 502, "CHECKOUT_SESSION_FAILED");
    }
}
