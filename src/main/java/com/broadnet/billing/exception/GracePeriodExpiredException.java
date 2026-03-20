package com.broadnet.billing.exception;

/**
 * Thrown when grace period for reactivation has expired
 * HTTP 400
 */
public class GracePeriodExpiredException extends BillingException {
    public GracePeriodExpiredException(Long companyId) {
        super("Grace period for reactivation has expired for company: " + companyId,
                400, "GRACE_PERIOD_EXPIRED");
    }
}
