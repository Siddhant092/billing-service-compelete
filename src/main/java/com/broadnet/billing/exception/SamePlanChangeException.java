package com.broadnet.billing.exception;

/**
 * Thrown when attempting to change to same plan
 * HTTP 400
 */
public class SamePlanChangeException extends BillingException {
    public SamePlanChangeException(String planCode) {
        super("Cannot change to same plan: " + planCode, 400, "SAME_PLAN_CHANGE");
    }
}
