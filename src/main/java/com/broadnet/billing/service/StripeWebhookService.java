package com.broadnet.billing.service;

public interface StripeWebhookService {
    public String processWebhook(String signature, String payload);
}
