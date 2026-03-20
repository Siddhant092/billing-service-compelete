package com.broadnet.billing.exception;

/**
 * Thrown when user lacks authorization
 * HTTP 403
 */
public class UnauthorizedAccessException extends BillingException {
    public UnauthorizedAccessException(String message) {
        super(message, 403, "UNAUTHORIZED_ACCESS");
    }
}
