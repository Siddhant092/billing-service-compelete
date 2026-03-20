package com.broadnet.billing.exception;

/**
 * Thrown when transaction fails
 * HTTP 500
 */
public class TransactionException extends BillingException {
    public TransactionException(String message) {
        super(message, 500, "TRANSACTION_FAILED");
    }

    public TransactionException(String message, Throwable cause) {
        super(message, cause, 500, "TRANSACTION_FAILED");
    }
}
