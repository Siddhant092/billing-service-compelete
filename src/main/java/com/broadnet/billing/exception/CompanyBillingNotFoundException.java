package com.broadnet.billing.exception;

/**
 * Thrown when company billing record not found
 * HTTP 404
 */
public class CompanyBillingNotFoundException extends BillingException {
    public CompanyBillingNotFoundException(Long companyId) {
        super("Company billing record not found: " + companyId, 404, "COMPANY_BILLING_NOT_FOUND");
    }
}
