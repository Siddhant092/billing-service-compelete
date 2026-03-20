package com.broadnet.billing.exception;

/**
 * Thrown when webhook processing fails
 * HTTP 500
 */
public class WebhookProcessingException extends BillingException {
    private final String eventType;
    private final String stripeEventId;

    public WebhookProcessingException(String message, String eventType, String stripeEventId) {
        super(message, 500, "WEBHOOK_PROCESSING_FAILED");
        this.eventType = eventType;
        this.stripeEventId = stripeEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }
}
