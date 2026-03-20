package com.broadnet.billing.exception;

/**
 * Thrown when payment method is invalid
 * HTTP 400
 */
public class InvalidPaymentMethodException extends BillingException {
    public InvalidPaymentMethodException(String message) {
        super(message, 400, "INVALID_PAYMENT_METHOD");
    }
}
