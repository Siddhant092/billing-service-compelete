package com.broadnet.billing.exception;

/**
 * Thrown when plan is inactive or not found
 * HTTP 400
 */
public class InactivePlanException extends BillingException {
    public InactivePlanException(String planCode) {
        super("Plan is inactive or not found: " + planCode, 400, "INACTIVE_PLAN");
    }
}
