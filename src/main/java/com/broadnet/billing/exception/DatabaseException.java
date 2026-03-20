package com.broadnet.billing.exception;

/**
 * Thrown when database operation fails
 * HTTP 500
 */
public class DatabaseException extends BillingException {
    public DatabaseException(String message) {
        super(message, 500, "DATABASE_ERROR");
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause, 500, "DATABASE_ERROR");
    }
}
