package com.broadnet.billing.exception;

/**
 * Thrown when Stripe object is not found
 * HTTP 404
 */
public class StripeObjectNotFoundException extends BillingException {
    public StripeObjectNotFoundException(String stripeId) {
        super("Stripe object not found: " + stripeId, 404, "STRIPE_OBJECT_NOT_FOUND");
    }
}
