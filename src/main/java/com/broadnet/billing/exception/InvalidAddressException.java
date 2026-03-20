package com.broadnet.billing.exception;

/**
 * Thrown when address validation fails
 * HTTP 400
 */
public class InvalidAddressException extends BillingException {
    public InvalidAddressException(String message) {
        super(message, 400, "INVALID_ADDRESS");
    }
}
