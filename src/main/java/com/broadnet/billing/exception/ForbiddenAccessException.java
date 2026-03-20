package com.broadnet.billing.exception;

/**
 * Thrown when resource access is forbidden
 * HTTP 403
 */
public class ForbiddenAccessException extends BillingException {
    public ForbiddenAccessException(String message) {
        super(message, 403, "FORBIDDEN_ACCESS");
    }
}
