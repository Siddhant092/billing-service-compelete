package com.broadnet.billing.exception;

/**
 * Thrown when enterprise pricing is not found
 * HTTP 404
 */
public class EnterprisePricingNotFoundException extends BillingException {
    public EnterprisePricingNotFoundException(Long companyId) {
        super("Enterprise pricing not found for company: " + companyId, 404, "ENTERPRISE_PRICING_NOT_FOUND");
    }
}
