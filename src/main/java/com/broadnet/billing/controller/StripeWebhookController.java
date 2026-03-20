package com.broadnet.billing.controller;

import com.broadnet.billing.service.StripeWebhookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {
    private final StripeWebhookService stripeWebhookService;
    public StripeWebhookController(StripeWebhookService stripeWebhookService) {
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestHeader("Stripe-Signature") String signature, @RequestBody String payload) {
        try {
            return ResponseEntity.ok(stripeWebhookService.processWebhook(signature, payload));
        }
        catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
