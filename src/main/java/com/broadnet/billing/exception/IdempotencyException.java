package com.broadnet.billing.exception;

/**
 * Thrown when idempotency check fails
 * HTTP 409
 */
public class IdempotencyException extends BillingException {
    public IdempotencyException(String stripeEventId) {
        super("Webhook already processed: " + stripeEventId, 409, "IDEMPOTENCY_CONFLICT");
    }
}
