package com.broadnet.billing.exception;

/**
 * Thrown when PDF generation fails
 * HTTP 500
 */
public class PdfGenerationException extends BillingException {
    public PdfGenerationException(String message) {
        super(message, 500, "PDF_GENERATION_FAILED");
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause, 500, "PDF_GENERATION_FAILED");
    }
}
