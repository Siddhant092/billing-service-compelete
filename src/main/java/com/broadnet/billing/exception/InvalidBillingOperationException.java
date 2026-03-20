package com.broadnet.billing.exception;

/**
 * Thrown when billing operation is not allowed
 * HTTP 400
 */
public class InvalidBillingOperationException extends BillingException {
    public InvalidBillingOperationException(String message) {
        super(message, 400, "INVALID_BILLING_OPERATION");
    }

    public InvalidBillingOperationException(String message, Throwable cause) {
        super(message, cause, 400, "INVALID_BILLING_OPERATION");
    }
}
