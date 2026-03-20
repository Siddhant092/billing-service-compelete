package com.broadnet.billing.service.impl;

import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.EmailNotificationException;
import com.broadnet.billing.exception.WebhookProcessingException;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.StripeWebhookService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.*;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

/**
 * ============================================================================
 * STRIPE WEBHOOK SERVICE IMPLEMENTATION
 * Processes Stripe webhook events and synchronizes billing state
 *
 * Architecture (aligned with Architecture_Plan.md):
 * 1. Verify webhook signature
 * 2. Store event (idempotency)
 * 3. Route to appropriate handler
 * 4. Update local database
 * 5. Create notifications
 *
 * Key Principles:
 * - Stripe is system of record for billing state
 * - Local DB is system of record for entitlements
 * - Webhooks trigger entitlement recalculation
 * - All operations are idempotent
 * ============================================================================
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class StripeWebhookServiceImpl implements StripeWebhookService {

    // Repositories (using existing ones from your codebase)
    private final BillingWebhookEventRepository webhookEventRepository;
    private final CompanyBillingRepository billingRepository;
    private final BillingInvoiceRepository invoiceRepository;
    private final BillingPaymentMethodRepository paymentMethodRepository;
    private final BillingNotificationRepository notificationRepository;
    private final BillingEntitlementHistoryRepository entitlementHistoryRepository;
    private final ObjectMapper objectMapper;
    private final JavaMailSender mailSender;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    // ===== EMAIL MESSAGE TEMPLATES =====
    private static final String PAYMENT_FAILED_SUBJECT = "Payment Failed — Action Required";
    private static final String PAYMENT_FAILED_MESSAGE =
            "Hello,\n\n" +
                    "We were unable to process your subscription payment.\n" +
                    "Please update your payment method to avoid service interruption.\n\n" +
                    "Best regards,\n" +
                    "HTI Support Team";

    private static final String TRIAL_ENDING_SUBJECT = "Trial Ending Soon";
    private static final String TRIAL_ENDING_MESSAGE =
            "Hello,\n\n" +
                    "Your trial period will end in 3 days.\n" +
                    "Please add a payment method to continue service without interruption.\n\n" +
                    "Best regards,\n" +
                    "HTI Support Team";

    private static final String SUBSCRIPTION_CANCELED_SUBJECT = "Subscription Canceled";
    private static final String SUBSCRIPTION_CANCELED_MESSAGE =
            "Hello,\n\n" +
                    "Your subscription has been canceled and is no longer active.\n" +
                    "If you have questions, please contact our support team.\n\n" +
                    "Best regards,\n" +
                    "HTI Support Team";

    private static final String SUBSCRIPTION_RENEWED_SUBJECT = "Subscription Renewed Successfully";
    private static final String SUBSCRIPTION_RENEWED_MESSAGE =
            "Hello,\n\n" +
                    "Your subscription has been renewed successfully.\n" +
                    "Thank you for your continued business.\n\n" +
                    "Best regards,\n" +
                    "HTI Support Team";

    private static final String INVOICE_CREATED_SUBJECT = "New Invoice Available";
    private static final String INVOICE_CREATED_MESSAGE =
            "Hello,\n\n" +
                    "A new invoice has been created for your account.\n" +
                    "You can view and download it from your billing dashboard.\n\n" +
                    "Best regards,\n" +
                    "HTI Support Team";

    /**
     * Main webhook processing entry point
     * Implements idempotent processing with signature verification
     */
    @Override
    @Transactional
    public String processWebhook(String signature, String payload) {
        Event event = null;

        // Step 1: Verify signature and construct event
        try {
            event = Webhook.constructEvent(payload, signature, webhookSecret);
            log.info("Received Stripe webhook: {} ({})", event.getType(), event.getId());
        } catch (SignatureVerificationException e) {
            log.warn("Invalid webhook signature: {}", e.getMessage());
//            throw new WebhookProcessingException("Invalid signature");
        } catch (Exception e) {
            log.error("Error constructing webhook event: {}", e.getMessage());
//            throw new WebhookProcessingException("Failed to parse webhook");
        }

        // Step 2: Check for duplicate (idempotency)
        if (webhookEventRepository.existsByStripeEventId(event.getId())) {
            log.info("Webhook {} already processed — skipping", event.getId());
            return "Event already processed";
        }

        // Step 3: Store webhook event for audit trail
        BillingWebhookEvent webhookEvent = storeWebhookEvent(event, payload);

        // Step 4: Route to handler
        try {
            routeEvent(event);
            webhookEvent.setProcessed(true);
            webhookEvent.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(webhookEvent);
            log.info("Webhook {} processed successfully", event.getId());
            return "Webhook processed successfully";
        } catch (WebhookProcessingException e) {
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setRetryCount(webhookEvent.getRetryCount() + 1);
            webhookEventRepository.save(webhookEvent);
            log.warn("Webhook {} processing failed: {}", event.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            webhookEvent.setErrorMessage(e.getMessage());
            webhookEvent.setRetryCount(webhookEvent.getRetryCount() + 1);
            webhookEventRepository.save(webhookEvent);
            log.error("Unexpected error processing webhook {}: {}", event.getId(), e.getMessage(), e);
//            throw new WebhookProcessingException("Processing failed: " + e.getMessage());
        }
        return "Event not processed";
    }

    /**
     * Store webhook event for idempotency and audit trail
     */
    private BillingWebhookEvent storeWebhookEvent(Event event, String payload) {
        BillingWebhookEvent webhookEvent = BillingWebhookEvent.builder()
                .stripeEventId(event.getId())
                .eventType(event.getType())
                .payload(payload)
                .processed(false)
                .retryCount(0)
                .build();

        // Extract Stripe IDs for better querying
        try {
            if (event.getType().startsWith("customer.subscription") ||
                    event.getType().startsWith("invoice")) {

                if (event.getType().startsWith("customer.subscription")) {
                    Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (subscription != null) {
                        webhookEvent.setStripeCustomerId(subscription.getCustomer());
                        webhookEvent.setStripeSubscriptionId(subscription.getId());
                    }
                } else if (event.getType().startsWith("invoice")) {
                    Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                            .getObject().orElse(null);
                    if (invoice != null) {
                        webhookEvent.setStripeCustomerId(invoice.getCustomer());
//                        webhookEvent.setStripeSubscriptionId(invoice.getSubscription());
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract Stripe IDs from event: {}", e.getMessage());
        }

        return webhookEventRepository.save(webhookEvent);
    }

    /**
     * Route event to appropriate handler based on event type
     */
    private void routeEvent(Event event) {
        switch (event.getType()) {
            // Subscription events
            case "customer.subscription.created" -> handleSubscriptionCreated(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            case "customer.subscription.trial_will_end" -> handleSubscriptionTrialWillEnd(event);

            // Invoice events
            case "invoice.created" -> handleInvoiceCreated(event);
            case "invoice.finalized" -> handleInvoiceFinalized(event);
            case "invoice.payment_succeeded" -> handleInvoicePaymentSucceeded(event);
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
            case "invoice.payment_action_required" -> handleInvoicePaymentActionRequired(event);
            case "invoice.voided" -> handleInvoiceVoided(event);
            case "invoice.marked_uncollectible" -> handleInvoiceMarkedUncollectible(event);

            // Payment Method events
            case "payment_method.attached" -> handlePaymentMethodAttached(event);
            case "payment_method.updated" -> handlePaymentMethodUpdated(event);
            case "payment_method.detached" -> handlePaymentMethodDetached(event);

            // Subscription Schedule events
            case "subscription_schedule.created" -> handleSubscriptionScheduleCreated(event);
            case "subscription_schedule.released" -> handleSubscriptionScheduleReleased(event);
            case "subscription_schedule.completed" -> handleSubscriptionScheduleCompleted(event);

            // Customer events
            case "customer.updated" -> handleCustomerUpdated(event);

            // Charge events (for audit)
            case "charge.succeeded" -> handleChargeSucceeded(event);
            case "charge.failed" -> handleChargeFailed(event);

            default -> log.debug("Unhandled webhook event type: {}", event.getType());
        }
    }

    // ===== SUBSCRIPTION EVENT HANDLERS =====

    /**
     * Handle customer.subscription.created
     * Triggered when a new subscription is created
     */
    private void handleSubscriptionCreated(Event event) {
        sendNotification("siddhantsinghrajpoot91@gmail.com", "TestMail", "test mail sent when subscription created");
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription.created: {}", subscription.getId());

        CompanyBilling billing = findBillingByStripeCustomer(subscription.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", subscription.getCustomer());
            return;
        }

        // Update subscription details
        billing.setStripeSubscriptionId(subscription.getId());
        billing.setSubscriptionStatus(mapStripeStatus(subscription.getStatus()));
        billing.setBillingInterval(mapBillingInterval(subscription.getItems().getData().get(0).getPlan().getInterval()));
//        billing.setPeriodStart(toLocalDateTime(subscription.getCurrentPeriodStart()));
//        billing.setPeriodEnd(toLocalDateTime(subscription.getCurrentPeriodEnd()));
        billing.setLastWebhookAt(LocalDateTime.now());

        billingRepository.save(billing);

        // Create notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.subscription_active,
                "Subscription Active",
                "Your subscription has been activated successfully",
                BillingNotification.Severity.success,
                event.getId());

        log.info("Subscription created for company: {}", billing.getCompanyId());
    }

    /**
     * Handle customer.subscription.updated
     * Triggered when subscription is modified (plan change, renewal, etc.)
     */
    private void handleSubscriptionUpdated(Event event) {
        sendNotification("siddhantsinghrajpoot91@gmail.com", "TestMail", "test mail sent when subscription updated");
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription.updated: {}", subscription.getId());

        CompanyBilling billing = findBillingByStripeSubscription(subscription.getId());
        if (billing == null) {
            log.warn("No billing record found for subscription: {}", subscription.getId());
            return;
        }

        // Store old status for comparison
        CompanyBilling.SubscriptionStatus oldStatus = billing.getSubscriptionStatus();

        // Update subscription state
        billing.setSubscriptionStatus(mapStripeStatus(subscription.getStatus()));
//        billing.setPeriodStart(toLocalDateTime(subscription.getCurrentPeriodStart()));
//        billing.setPeriodEnd(toLocalDateTime(subscription.getCurrentPeriodEnd()));
        billing.setCancelAtPeriodEnd(subscription.getCancelAtPeriodEnd());

        if (subscription.getCanceledAt() != null) {
            billing.setCanceledAt(toLocalDateTime(subscription.getCanceledAt()));
        }
        if (subscription.getCancelAt() != null) {
            billing.setCancelAt(toLocalDateTime(subscription.getCancelAt()));
        }

        billing.setLastWebhookAt(LocalDateTime.now());
        billingRepository.save(billing);

        // Create notification if status changed to active or renewed
        if (oldStatus != billing.getSubscriptionStatus() &&
                billing.getSubscriptionStatus() == CompanyBilling.SubscriptionStatus.active) {
            String message = "Your subscription has been renewed successfully";
            createNotification(billing.getCompanyId(),
                    BillingNotification.NotificationType.subscription_renewed,
                    "Subscription Renewed",
                    message,
                    BillingNotification.Severity.success,
                    event.getId());

            // Send subscription renewed email
            String customerEmail = getCustomerEmail(subscription.getCustomer());
            if (customerEmail != null) {
                sendNotification(customerEmail, SUBSCRIPTION_RENEWED_SUBJECT, SUBSCRIPTION_RENEWED_MESSAGE);
            }
        } else {
            // Send payment failed email for other updates
            String customerEmail = getCustomerEmail(subscription.getCustomer());
            if (customerEmail != null) {
                sendNotification(customerEmail, PAYMENT_FAILED_SUBJECT, PAYMENT_FAILED_MESSAGE);
            }
        }

        log.info("Subscription updated for company: {}", billing.getCompanyId());
    }

    /**
     * Handle customer.subscription.deleted
     * Triggered when subscription is canceled and expires
     */
    private void handleSubscriptionDeleted(Event event) {
        sendNotification("siddhantsinghrajpoot91@gmail.com", "TestMail", "test mail sent when subscription has been deleted");
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription.deleted: {}", subscription.getId());

        CompanyBilling billing = findBillingByStripeSubscription(subscription.getId());
        if (billing == null) {
            log.warn("No billing record found for subscription: {}", subscription.getId());
            return;
        }

        // Update to canceled status
        billing.setSubscriptionStatus(CompanyBilling.SubscriptionStatus.canceled);
        billing.setCanceledAt(LocalDateTime.now());
        billing.setLastWebhookAt(LocalDateTime.now());

        billingRepository.save(billing);

        // Create notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.subscription_canceled,
                "Subscription Canceled",
                "Your subscription has been canceled and is no longer active",
                BillingNotification.Severity.warning,
                event.getId());

        // Send subscription canceled email
        String customerEmail = getCustomerEmail(subscription.getCustomer());
        if (customerEmail != null) {
            sendNotification(customerEmail, SUBSCRIPTION_CANCELED_SUBJECT, SUBSCRIPTION_CANCELED_MESSAGE);
        }

        log.info("Subscription deleted for company: {}", billing.getCompanyId());
    }

    /**
     * Handle customer.subscription.trial_will_end
     * Triggered 3 days before trial expires
     */
    private void handleSubscriptionTrialWillEnd(Event event) {
        sendNotification("siddhantsinghrajpoot91@gmail.com", "TestMail", "test mail sent when subscription trial going to end");
        Subscription subscription = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription.trial_will_end: {}", subscription.getId());

        CompanyBilling billing = findBillingByStripeSubscription(subscription.getId());
        if (billing == null) {
            log.warn("No billing record found for subscription: {}", subscription.getId());
            return;
        }

        // Create warning notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.payment_method_expired,
                "Trial Ending Soon",
                "Your trial period will end in 3 days. Please add a payment method to continue service",
                BillingNotification.Severity.warning,
                event.getId());

        // Send trial ending email
        String customerEmail = getCustomerEmail(subscription.getCustomer());
        if (customerEmail != null) {
            sendNotification(customerEmail, TRIAL_ENDING_SUBJECT, TRIAL_ENDING_MESSAGE);
        }

        log.info("Trial ending notification sent for company: {}", billing.getCompanyId());
    }

    // ===== INVOICE EVENT HANDLERS =====

    /**
     * Handle invoice.created
     * Triggered when Stripe creates a new invoice
     */
    private void handleInvoiceCreated(Event event) {
        sendNotification("siddhantsinghrajpoot91@gmail.com", "TestMail", "test mail sent when invoice created");
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.created: {}", invoice.getId());

        CompanyBilling billing = findBillingByStripeCustomer(invoice.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", invoice.getCustomer());
            return;
        }

        // Create or update invoice record
        BillingInvoice billingInvoice = invoiceRepository
                .findByStripeInvoiceId(invoice.getId())
                .orElse(new BillingInvoice());

        billingInvoice.setCompanyId(billing.getCompanyId());
        billingInvoice.setStripeInvoiceId(invoice.getId());
        billingInvoice.setInvoiceNumber(invoice.getNumber());
        billingInvoice.setStatus(mapInvoiceStatus(invoice.getStatus()));
        billingInvoice.setAmountDue(invoice.getAmountDue() != null ? invoice.getAmountDue().intValue() : 0);
        billingInvoice.setAmountPaid(invoice.getAmountPaid() != null ? invoice.getAmountPaid().intValue() : 0);
        billingInvoice.setSubtotal(invoice.getSubtotal() != null ? invoice.getSubtotal().intValue() : 0);
//        billingInvoice.setTaxAmount(invoice.getTax() != null ? invoice.getTax().intValue() : 0);
        billingInvoice.setTotal(invoice.getTotal() != null ? invoice.getTotal().intValue() : 0);
        billingInvoice.setCurrency(invoice.getCurrency());
        billingInvoice.setInvoiceDate(toLocalDateTime(invoice.getCreated()));
        billingInvoice.setDueDate(invoice.getDueDate() != null ? toLocalDateTime(invoice.getDueDate()) : null);
        billingInvoice.setPeriodStart(invoice.getPeriodStart() != null ? toLocalDateTime(invoice.getPeriodStart()) : null);
        billingInvoice.setPeriodEnd(invoice.getPeriodEnd() != null ? toLocalDateTime(invoice.getPeriodEnd()) : null);
        billingInvoice.setHostedInvoiceUrl(invoice.getHostedInvoiceUrl());
        billingInvoice.setInvoicePdfUrl(invoice.getInvoicePdf());

        // Store line items as JSON
        try {
            billingInvoice.setLineItems(objectMapper.writeValueAsString(invoice.getLines().getData()));
        } catch (Exception e) {
            log.error("Error serializing line items: {}", e.getMessage());
            billingInvoice.setLineItems("[]");
        }

        invoiceRepository.save(billingInvoice);

        // Create notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.invoice_created,
                "New Invoice",
                String.format("Invoice for $%.2f has been created", invoice.getTotal() / 100.0),
                BillingNotification.Severity.info,
                event.getId());

        // Send invoice created email
        String customerEmail = invoice.getCustomerEmail();
        if (customerEmail != null) {
            sendNotification(customerEmail, INVOICE_CREATED_SUBJECT, INVOICE_CREATED_MESSAGE);
        }

        log.info("Invoice created for company: {}", billing.getCompanyId());
    }

    /**
     * Handle invoice.finalized
     * Triggered when invoice is finalized and ready for payment
     */
    private void handleInvoiceFinalized(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.finalized: {}", invoice.getId());

        Optional<BillingInvoice> invoiceOpt = invoiceRepository.findByStripeInvoiceId(invoice.getId());
        if (invoiceOpt.isEmpty()) {
            log.warn("Invoice not found: {}", invoice.getId());
            return;
        }

        BillingInvoice billingInvoice = invoiceOpt.get();
        billingInvoice.setStatus(BillingInvoice.InvoiceStatus.open);
        invoiceRepository.save(billingInvoice);

        log.info("Invoice finalized: {}", invoice.getId());
    }

    /**
     * Handle invoice.payment_succeeded
     * Triggered when payment is successful
     */
    private void handleInvoicePaymentSucceeded(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.payment_succeeded: {}", invoice.getId());

        CompanyBilling billing = findBillingByStripeCustomer(invoice.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", invoice.getCustomer());
            return;
        }

        // Update invoice
        Optional<BillingInvoice> invoiceOpt = invoiceRepository.findByStripeInvoiceId(invoice.getId());
        if (invoiceOpt.isPresent()) {
            BillingInvoice billingInvoice = invoiceOpt.get();
            billingInvoice.setStatus(BillingInvoice.InvoiceStatus.paid);
            billingInvoice.setPaidAt(LocalDateTime.now());
            billingInvoice.setAmountPaid(invoice.getAmountPaid() != null ? invoice.getAmountPaid().intValue() : 0);
            invoiceRepository.save(billingInvoice);
        }

        // Clear payment failure flags
        billing.setPaymentFailureDate(null);
        billing.setServiceRestrictedAt(null);
        billing.setRestrictionReason(null);
        billing.setLastWebhookAt(LocalDateTime.now());

        // Update subscription status to active if it was past_due
        if (billing.getSubscriptionStatus() == CompanyBilling.SubscriptionStatus.past_due) {
            billing.setSubscriptionStatus(CompanyBilling.SubscriptionStatus.active);
        }

        billingRepository.save(billing);

        // Create success notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.payment_succeeded,
                "Payment Successful",
                String.format("Payment of $%.2f received successfully", invoice.getAmountPaid() / 100.0),
                BillingNotification.Severity.success,
                event.getId());

        log.info("Payment succeeded for company: {}", billing.getCompanyId());
    }

    /**
     * Handle invoice.payment_failed
     * Triggered when payment fails
     */
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.payment_failed: {}", invoice.getId());

        CompanyBilling billing = findBillingByStripeCustomer(invoice.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", invoice.getCustomer());
            return;
        }

        // Update billing with failure info
        billing.setPaymentFailureDate(LocalDateTime.now());
        billing.setSubscriptionStatus(CompanyBilling.SubscriptionStatus.past_due);
        billing.setLastWebhookAt(LocalDateTime.now());
        billingRepository.save(billing);

        // Create error notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.payment_failed,
                "Payment Failed",
                "Your payment could not be processed. Please update your payment method to avoid service interruption",
                BillingNotification.Severity.error,
                event.getId());

        // Send payment failed email
        String customerEmail = invoice.getCustomerEmail();
        if (customerEmail != null) {
            sendNotification(customerEmail, PAYMENT_FAILED_SUBJECT, PAYMENT_FAILED_MESSAGE);
        }

        log.info("Payment failed for company: {}", billing.getCompanyId());
    }

    /**
     * Handle invoice.payment_action_required
     * Triggered when payment requires additional action (3D Secure, etc.)
     */
    private void handleInvoicePaymentActionRequired(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.payment_action_required: {}", invoice.getId());

        CompanyBilling billing = findBillingByStripeCustomer(invoice.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", invoice.getCustomer());
            return;
        }

        // Create warning notification
        createNotification(billing.getCompanyId(),
                BillingNotification.NotificationType.payment_failed,
                "Payment Action Required",
                "Additional authentication is required to process your payment. Please complete the verification.",
                BillingNotification.Severity.warning,
                event.getId());

        log.info("Payment action required for company: {}", billing.getCompanyId());
    }

    /**
     * Handle invoice.voided
     * Triggered when invoice is voided
     */
    private void handleInvoiceVoided(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.voided: {}", invoice.getId());

        Optional<BillingInvoice> invoiceOpt = invoiceRepository.findByStripeInvoiceId(invoice.getId());
        if (invoiceOpt.isEmpty()) {
            log.warn("Invoice not found: {}", invoice.getId());
            return;
        }

        BillingInvoice billingInvoice = invoiceOpt.get();
        billingInvoice.setStatus(BillingInvoice.InvoiceStatus.uncollectible);
        invoiceRepository.save(billingInvoice);

        log.info("Invoice voided: {}", invoice.getId());
    }

    /**
     * Handle invoice.marked_uncollectible
     * Triggered when invoice is marked as uncollectible
     */
    private void handleInvoiceMarkedUncollectible(Event event) {
        Invoice invoice = (Invoice) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing invoice.marked_uncollectible: {}", invoice.getId());

        Optional<BillingInvoice> invoiceOpt = invoiceRepository.findByStripeInvoiceId(invoice.getId());
        if (invoiceOpt.isEmpty()) {
            log.warn("Invoice not found: {}", invoice.getId());
            return;
        }

        BillingInvoice billingInvoice = invoiceOpt.get();
        billingInvoice.setStatus(BillingInvoice.InvoiceStatus.uncollectible);
        invoiceRepository.save(billingInvoice);

        log.info("Invoice marked uncollectible: {}", invoice.getId());
    }

    // ===== PAYMENT METHOD EVENT HANDLERS =====

    /**
     * Handle payment_method.attached
     * Triggered when payment method is attached to customer
     */
    private void handlePaymentMethodAttached(Event event) {
        PaymentMethod paymentMethod = (PaymentMethod) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing payment_method.attached: {}", paymentMethod.getId());

        CompanyBilling billing = findBillingByStripeCustomer(paymentMethod.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", paymentMethod.getCustomer());
            return;
        }

        // Create or update payment method record
        BillingPaymentMethod billingPm = paymentMethodRepository
                .findByStripePaymentMethodId(paymentMethod.getId())
                .orElse(new BillingPaymentMethod());

        billingPm.setCompanyId(billing.getCompanyId());
        billingPm.setStripePaymentMethodId(paymentMethod.getId());
        billingPm.setType(BillingPaymentMethod.PaymentMethodType.valueOf(paymentMethod.getType()));

        if ("card".equals(paymentMethod.getType()) && paymentMethod.getCard() != null) {
            billingPm.setCardBrand(paymentMethod.getCard().getBrand());
            billingPm.setCardLast4(paymentMethod.getCard().getLast4());
            billingPm.setCardExpMonth(paymentMethod.getCard().getExpMonth() != null ?
                    paymentMethod.getCard().getExpMonth().intValue() : null);
            billingPm.setCardExpYear(paymentMethod.getCard().getExpYear() != null ?
                    paymentMethod.getCard().getExpYear().intValue() : null);
        }

        // Store billing details as JSON
        try {
            if (paymentMethod.getBillingDetails() != null) {
                billingPm.setBillingDetails(objectMapper.writeValueAsString(paymentMethod.getBillingDetails()));
            }
        } catch (Exception e) {
            log.error("Error serializing billing details: {}", e.getMessage());
        }

        paymentMethodRepository.save(billingPm);

        log.info("Payment method attached for company: {}", billing.getCompanyId());
    }

    /**
     * Handle payment_method.updated
     * Triggered when payment method details are updated
     */
    private void handlePaymentMethodUpdated(Event event) {
        PaymentMethod paymentMethod = (PaymentMethod) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing payment_method.updated: {}", paymentMethod.getId());

        Optional<BillingPaymentMethod> pmOpt = paymentMethodRepository
                .findByStripePaymentMethodId(paymentMethod.getId());

        if (pmOpt.isEmpty()) {
            log.warn("Payment method not found: {}", paymentMethod.getId());
            return;
        }

        BillingPaymentMethod billingPm = pmOpt.get();

        // Update card details if card type
        if ("card".equals(paymentMethod.getType()) && paymentMethod.getCard() != null) {
            billingPm.setCardExpMonth(paymentMethod.getCard().getExpMonth() != null ?
                    paymentMethod.getCard().getExpMonth().intValue() : null);
            billingPm.setCardExpYear(paymentMethod.getCard().getExpYear() != null ?
                    paymentMethod.getCard().getExpYear().intValue() : null);
        }

        paymentMethodRepository.save(billingPm);

        log.info("Payment method updated: {}", paymentMethod.getId());
    }

    /**
     * Handle payment_method.detached
     * Triggered when payment method is removed from customer
     */
    private void handlePaymentMethodDetached(Event event) {
        PaymentMethod paymentMethod = (PaymentMethod) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing payment_method.detached: {}", paymentMethod.getId());

        paymentMethodRepository.findByStripePaymentMethodId(paymentMethod.getId())
                .ifPresent(paymentMethodRepository::delete);

        log.info("Payment method detached: {}", paymentMethod.getId());
    }

    // ===== SUBSCRIPTION SCHEDULE EVENT HANDLERS =====

    /**
     * Handle subscription_schedule.created
     * Triggered when a subscription schedule is created
     */
    private void handleSubscriptionScheduleCreated(Event event) {
        SubscriptionSchedule schedule = (SubscriptionSchedule) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription_schedule.created: {}", schedule.getId());

        CompanyBilling billing = findBillingByStripeCustomer(schedule.getCustomer());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", schedule.getCustomer());
            return;
        }

        billing.setStripeScheduleId(schedule.getId());
        billingRepository.save(billing);

        log.info("Subscription schedule created for company: {}", billing.getCompanyId());
    }

    /**
     * Handle subscription_schedule.released
     * Triggered when schedule is released and subscription continues normally
     */
    private void handleSubscriptionScheduleReleased(Event event) {
        SubscriptionSchedule schedule = (SubscriptionSchedule) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription_schedule.released: {}", schedule.getId());

        billingRepository.findByStripeCustomerId(schedule.getCustomer())
                .ifPresent(billing -> {
                    billing.setStripeScheduleId(null);
                    billingRepository.save(billing);
                    log.info("Subscription schedule released for company: {}", billing.getCompanyId());
                });
    }

    /**
     * Handle subscription_schedule.completed
     * Triggered when all phases of schedule are completed
     */
    private void handleSubscriptionScheduleCompleted(Event event) {
        SubscriptionSchedule schedule = (SubscriptionSchedule) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing subscription_schedule.completed: {}", schedule.getId());

        billingRepository.findByStripeCustomerId(schedule.getCustomer())
                .ifPresent(billing -> {
                    billing.setStripeScheduleId(null);
                    billingRepository.save(billing);
                    log.info("Subscription schedule completed for company: {}", billing.getCompanyId());
                });
    }

    // ===== CUSTOMER EVENT HANDLERS =====

    /**
     * Handle customer.updated
     * Triggered when customer details are updated
     */
    private void handleCustomerUpdated(Event event) {
        Customer customer = (Customer) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing customer.updated: {}", customer.getId());

        CompanyBilling billing = findBillingByStripeCustomer(customer.getId());
        if (billing == null) {
            log.warn("No billing record found for customer: {}", customer.getId());
            return;
        }

        billing.setLastWebhookAt(LocalDateTime.now());
        billingRepository.save(billing);

        log.info("Customer updated for company: {}", billing.getCompanyId());
    }

    // ===== CHARGE EVENT HANDLERS =====

    /**
     * Handle charge.succeeded
     * Triggered when a charge is successful (for audit trail)
     */
    private void handleChargeSucceeded(Event event) {
        Charge charge = (Charge) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.info("Processing charge.succeeded: {} (amount: {})",
                charge.getId(), charge.getAmount());
        // Most charge handling is done via invoice events
        // This is mainly for logging/auditing
    }

    /**
     * Handle charge.failed
     * Triggered when a charge fails (for audit trail)
     */
    private void handleChargeFailed(Event event) {
        Charge charge = (Charge) event.getDataObjectDeserializer()
                .getObject().orElseThrow();

        log.warn("Processing charge.failed: {} (amount: {}, reason: {})",
                charge.getId(), charge.getAmount(), charge.getFailureMessage());
        // Most charge handling is done via invoice events
        // This is mainly for logging/auditing
    }

    // ===== HELPER METHODS =====

    /**
     * Find CompanyBilling by Stripe customer ID
     */
    private CompanyBilling findBillingByStripeCustomer(String customerId) {
        return billingRepository.findByStripeCustomerId(customerId).orElse(null);
    }

    /**
     * Find CompanyBilling by Stripe subscription ID
     */
    private CompanyBilling findBillingByStripeSubscription(String subscriptionId) {
        return billingRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
    }

    /**
     * Get customer email from Stripe customer ID
     * Retrieves the customer object from Stripe API
     *
     * @param customerId Stripe customer ID
     * @return Customer email or null if not found
     */
    private String getCustomerEmail(String customerId) {
        try {
            Customer customer = Customer.retrieve(customerId);
            return customer.getEmail();
        } catch (Exception e) {
            log.warn("Could not retrieve email for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    /**
     * Map Stripe subscription status to our enum
     */
    private CompanyBilling.SubscriptionStatus mapStripeStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "active" -> CompanyBilling.SubscriptionStatus.active;
            case "past_due" -> CompanyBilling.SubscriptionStatus.past_due;
            case "canceled" -> CompanyBilling.SubscriptionStatus.canceled;
            case "trialing" -> CompanyBilling.SubscriptionStatus.trialing;
            case "unpaid" -> CompanyBilling.SubscriptionStatus.unpaid;
            case "incomplete" -> CompanyBilling.SubscriptionStatus.incomplete;
            case "incomplete_expired" -> CompanyBilling.SubscriptionStatus.incomplete_expired;
            default -> CompanyBilling.SubscriptionStatus.active;
        };
    }

    /**
     * Map Stripe invoice status to our enum
     */
    private BillingInvoice.InvoiceStatus mapInvoiceStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "draft" -> BillingInvoice.InvoiceStatus.draft;
            case "open" -> BillingInvoice.InvoiceStatus.open;
            case "paid" -> BillingInvoice.InvoiceStatus.paid;
            case "void", "uncollectible" -> BillingInvoice.InvoiceStatus.uncollectible;
            default -> BillingInvoice.InvoiceStatus.draft;
        };
    }

    /**
     * Map Stripe billing interval to our enum
     */
    private CompanyBilling.BillingInterval mapBillingInterval(String interval) {
        return "year".equals(interval) ?
                CompanyBilling.BillingInterval.year :
                CompanyBilling.BillingInterval.month;
    }

    /**
     * Convert Unix timestamp to LocalDateTime
     */
    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) return null;
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault()
        );
    }

    /**
     * Create notification (default severity: info)
     */
    private void createNotification(Long companyId,
                                    BillingNotification.NotificationType type,
                                    String title,
                                    String message,
                                    String stripeEventId) {
        createNotification(companyId, type, title, message, BillingNotification.Severity.info, stripeEventId);
    }

    /**
     * Create notification with custom severity
     */
    private void createNotification(Long companyId,
                                    BillingNotification.NotificationType type,
                                    String title,
                                    String message,
                                    BillingNotification.Severity severity,
                                    String stripeEventId) {
        try {
            BillingNotification notification = BillingNotification.builder()
                    .companyId(companyId)
                    .notificationType(type)
                    .title(title)
                    .message(message)
                    .severity(severity)
                    .isRead(false)
                    .stripeEventId(stripeEventId)
                    .build();

            notificationRepository.save(notification);
            log.debug("Created notification for company {}: {}", companyId, title);
        } catch (Exception e) {
            log.error("Failed to create notification for company {}: {}", companyId, e.getMessage());
            // Don't throw - notification failure shouldn't break webhook processing
        }
    }

    /**
     * Send email notification
     * Wraps email sending with exception handling
     *
     * @param toEmail Email address to send to
     * @param subject Email subject line
     * @param body Email body content
     * @throws EmailNotificationException if sending fails
     */
    private void sendNotification(String toEmail, String subject, String body) {
        if (toEmail == null || toEmail.trim().isEmpty()) {
            log.warn("Email address is null or empty, skipping notification");
            return;
        }

        log.info("Sending email to '{}': {}", toEmail, subject);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("siddhantr.hti@hti.com");
            message.setTo("siddhantsinghrajpoot91@gmail.com");
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to {}", toEmail);
        } catch (MailException e) {
            log.error("Mail server error while sending to {}: {}", toEmail, e.getMessage());
            throw new EmailNotificationException(
                    "Mail server error while sending to " + toEmail + ": " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}: {}", toEmail, e.getMessage());
            throw new EmailNotificationException(
                    "Unexpected error while sending email to " + toEmail + ": " + e.getMessage(), e);
        }
    }
}