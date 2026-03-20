package com.broadnet.billing.exception;

/**
 * Thrown when addon is not found
 * HTTP 404
 */
public class BillingAddonNotFoundException extends BillingException {
    public BillingAddonNotFoundException(String addonCode) {
        super("Billing addon not found: " + addonCode, 404, "ADDON_NOT_FOUND");
    }
}
