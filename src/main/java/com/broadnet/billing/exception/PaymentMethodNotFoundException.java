package com.broadnet.billing.exception;

/**
 * Thrown when payment method is not found
 * HTTP 404
 */
public class PaymentMethodNotFoundException extends BillingException {
    public PaymentMethodNotFoundException(Long companyId) {
        super("Payment method not found for company: " + companyId, 404, "PAYMENT_METHOD_NOT_FOUND");
    }
}
