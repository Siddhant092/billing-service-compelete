package com.broadnet.billing.exception;

/**
 * Thrown when invoice is not found
 * HTTP 404
 */
public class BillingInvoiceNotFoundException extends BillingException {
    public BillingInvoiceNotFoundException(Long invoiceId) {
        super("Invoice not found: " + invoiceId, 404, "INVOICE_NOT_FOUND");
    }
}
