package com.broadnet.billing.exception;

/**
 * ============================================================================
 * EXCEPTION CLASSES
 * Custom exceptions for billing system with appropriate HTTP status codes
 * ============================================================================
 */

// ===== Base Billing Exception =====
public abstract class BillingException extends RuntimeException {

    private final int httpStatus;
    private final String errorCode;

    public BillingException(String message, int httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public BillingException(String message, Throwable cause, int httpStatus, String errorCode) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }
}