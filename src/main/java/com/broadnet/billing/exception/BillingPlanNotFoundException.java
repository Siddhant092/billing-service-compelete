package com.broadnet.billing.exception;

/**
 * Thrown when a billing entity is not found
 * HTTP 404
 */
public class BillingPlanNotFoundException extends BillingException {
    public BillingPlanNotFoundException(String planCode) {
        super("Billing plan not found: " + planCode, 404, "PLAN_NOT_FOUND");
    }

    public BillingPlanNotFoundException(Long planId) {
        super("Billing plan not found with ID: " + planId, 404, "PLAN_NOT_FOUND");
    }
}
