package com.broadnet.billing.exception;

/**
 * Thrown when usage limit is exceeded
 * HTTP 403
 */
public class UsageLimitExceededException extends BillingException {
    private final int used;
    private final int limit;

    public UsageLimitExceededException(String usageType, int used, int limit) {
        super(String.format("Usage limit exceeded for %s: %d/%d", usageType, used, limit),
                403, "USAGE_LIMIT_EXCEEDED");
        this.used = used;
        this.limit = limit;
    }

    public int getUsed() {
        return used;
    }

    public int getLimit() {
        return limit;
    }
}
