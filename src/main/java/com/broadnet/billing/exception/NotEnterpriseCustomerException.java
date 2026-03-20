package com.broadnet.billing.exception;

/**
 * Thrown when not an enterprise customer
 * HTTP 403
 */
public class NotEnterpriseCustomerException extends BillingException {
    public NotEnterpriseCustomerException(Long companyId) {
        super("Company is not an enterprise customer: " + companyId, 403, "NOT_ENTERPRISE_CUSTOMER");
    }
}
