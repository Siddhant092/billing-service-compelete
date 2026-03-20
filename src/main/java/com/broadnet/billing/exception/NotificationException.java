package com.broadnet.billing.exception;

/**
 * Thrown when notification creation fails
 * HTTP 500
 */
public class NotificationException extends BillingException {
    public NotificationException(String message) {
        super(message, 500, "NOTIFICATION_FAILED");
    }

    public NotificationException(String message, Throwable cause) {
        super(message, cause, 500, "NOTIFICATION_FAILED");
    }
}
