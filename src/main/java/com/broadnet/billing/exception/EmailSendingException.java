package com.broadnet.billing.exception;

/**
 * Thrown when email sending fails
 * HTTP 500
 */
public class EmailSendingException extends BillingException {
    public EmailSendingException(String message) {
        super(message, 500, "EMAIL_SENDING_FAILED");
    }

    public EmailSendingException(String message, Throwable cause) {
        super(message, cause, 500, "EMAIL_SENDING_FAILED");
    }
}
