package com.broadnet.billing.exception;

/**
 * Thrown when plan change is invalid
 * HTTP 400
 */
public class InvalidPlanChangeException extends BillingException {
    public InvalidPlanChangeException(String message) {
        super(message, 400, "INVALID_PLAN_CHANGE");
    }
}
