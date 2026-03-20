package com.broadnet.billing.exception;

/**
 * Thrown when Stripe API call fails
 * HTTP 502
 */
public class StripeApiException extends BillingException {
    private final String stripeErrorCode;

    public StripeApiException(String message, String stripeErrorCode) {
        super(message, 502, "STRIPE_API_ERROR");
        this.stripeErrorCode = stripeErrorCode;
    }

    public StripeApiException(String message, String stripeErrorCode, Throwable cause) {
        super(message, cause, 502, "STRIPE_API_ERROR");
        this.stripeErrorCode = stripeErrorCode;
    }

    public String getStripeErrorCode() {
        return stripeErrorCode;
    }
}
