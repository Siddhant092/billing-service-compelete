package com.broadnet.billing.exception;

/**
 * Thrown when Stripe payment is declined
 * HTTP 402
 */
public class StripePaymentDeclinedException extends BillingException {
    private final String declineCode;

    public StripePaymentDeclinedException(String message, String declineCode) {
        super(message, 402, "PAYMENT_DECLINED");
        this.declineCode = declineCode;
    }

    public String getDeclineCode() {
        return declineCode;
    }
}
