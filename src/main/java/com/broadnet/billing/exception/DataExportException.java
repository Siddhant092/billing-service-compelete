package com.broadnet.billing.exception;

/**
 * Thrown when data export fails
 * HTTP 500
 */
public class DataExportException extends BillingException {
    public DataExportException(String message) {
        super(message, 500, "EXPORT_FAILED");
    }

    public DataExportException(String message, Throwable cause) {
        super(message, cause, 500, "EXPORT_FAILED");
    }
}
