package com.broadnet.billing.exception;

/**
 * Thrown when operation conflicts with existing state
 * HTTP 409
 */
public class ConflictingOperationException extends BillingException {
    public ConflictingOperationException(String message) {
        super(message, 409, "CONFLICTING_OPERATION");
    }
}
