package com.broadnet.billing.exception;

public class EmailNotificationException extends RuntimeException {
    public EmailNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
