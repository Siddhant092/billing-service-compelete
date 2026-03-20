package com.broadnet.billing.exception;

/**
 * Thrown when addon is inactive or not found
 * HTTP 400
 */
public class InactiveAddonException extends BillingException {
    public InactiveAddonException(String addonCode) {
        super("Addon is inactive or not found: " + addonCode, 400, "INACTIVE_ADDON");
    }
}
