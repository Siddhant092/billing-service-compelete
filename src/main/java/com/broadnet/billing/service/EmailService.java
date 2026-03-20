package com.broadnet.billing.service;

import com.broadnet.billing.dto.EmailNotificationDTO;

/**
 * Email notification service for billing events
 * Supports various email types triggered by Stripe webhook events
 */
public interface EmailService {

    /**
     * Send payment success email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param amount Payment amount in cents
     * @param invoiceNumber Invoice number
     * @param invoiceUrl Invoice PDF URL
     */
    void sendPaymentSuccessEmail(Long companyId, String customerEmail, Integer amount,
                                 String invoiceNumber, String invoiceUrl);

    /**
     * Send payment failed email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param amount Attempted payment amount in cents
     * @param failureReason Reason for failure
     * @param retryDate Next retry date (if applicable)
     */
    void sendPaymentFailedEmail(Long companyId, String customerEmail, Integer amount,
                                String failureReason, String retryDate);

    /**
     * Send subscription activated email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param planName Plan name
     * @param billingInterval Billing interval (month/year)
     */
    void sendSubscriptionActivatedEmail(Long companyId, String customerEmail,
                                        String planName, String billingInterval);

    /**
     * Send subscription canceled email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param planName Plan name
     * @param endDate Subscription end date
     */
    void sendSubscriptionCanceledEmail(Long companyId, String customerEmail,
                                       String planName, String endDate);

    /**
     * Send subscription renewed email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param planName Plan name
     * @param nextBillingDate Next billing date
     */
    void sendSubscriptionRenewedEmail(Long companyId, String customerEmail,
                                      String planName, String nextBillingDate);

    /**
     * Send plan change email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param oldPlanName Old plan name
     * @param newPlanName New plan name
     * @param effectiveDate Effective date
     */
    void sendPlanChangeEmail(Long companyId, String customerEmail,
                             String oldPlanName, String newPlanName, String effectiveDate);

    /**
     * Send invoice created email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param amount Invoice amount in cents
     * @param dueDate Due date
     * @param invoiceUrl Invoice URL
     */
    void sendInvoiceCreatedEmail(Long companyId, String customerEmail, Integer amount,
                                 String dueDate, String invoiceUrl);

    /**
     * Send payment method expiring email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param cardLast4 Last 4 digits of card
     * @param expiryDate Expiry date
     */
    void sendPaymentMethodExpiringEmail(Long companyId, String customerEmail,
                                        String cardLast4, String expiryDate);

    /**
     * Send trial ending soon email
     * @param companyId Company ID
     * @param customerEmail Customer email address
     * @param daysRemaining Days remaining in trial
     * @param planName Plan name
     */
    void sendTrialEndingSoonEmail(Long companyId, String customerEmail,
                                  int daysRemaining, String planName);

    /**
     * Send generic notification email
     * @param emailDTO Email notification data transfer object
     */
    void sendNotificationEmail(EmailNotificationDTO emailDTO);
}