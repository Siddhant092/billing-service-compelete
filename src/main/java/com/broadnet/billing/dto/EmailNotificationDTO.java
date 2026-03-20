package com.broadnet.billing.dto;

import lombok.*;

/**
 * Data Transfer Object for email notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationDTO {

    private Long companyId;
    private String toEmail;
    private String subject;
    private String body;
    private EmailType emailType;

    // Optional fields for templating
    private String customerName;
    private String planName;
    private String invoiceNumber;
    private String invoiceUrl;
    private Integer amountCents;
    private String currency;
    private String billingInterval;
    private String effectiveDate;
    private String dueDate;
    private String failureReason;
    private String cardLast4;
    private String expiryDate;

    public enum EmailType {
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        SUBSCRIPTION_ACTIVATED,
        SUBSCRIPTION_CANCELED,
        SUBSCRIPTION_RENEWED,
        PLAN_CHANGED,
        INVOICE_CREATED,
        PAYMENT_METHOD_EXPIRING,
        TRIAL_ENDING_SOON,
        GENERIC_NOTIFICATION
    }

    /**
     * Format amount in cents to currency string
     */
    public String getFormattedAmount() {
        if (amountCents == null) return "$0.00";
        String curr = currency != null ? currency.toUpperCase() : "USD";
        double amount = amountCents / 100.0;
        return String.format("%s %.2f", getCurrencySymbol(curr), amount);
    }

    private String getCurrencySymbol(String currency) {
        return switch (currency) {
            case "EUR" -> "€";
            case "GBP" -> "£";
            case "INR" -> "₹";
            default -> "$";
        };
    }
}