package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_notifications",
        indexes = {
                @Index(name = "idx_company_unread", columnList = "company_id, is_read, created_at"),
                @Index(name = "idx_expires", columnList = "expires_at"),
                @Index(name = "idx_stripe_event", columnList = "stripe_event_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Company reference (high volume → keep as ID)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ===== TYPE =====
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    // ===== SEVERITY =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Severity severity = Severity.info;

    // ===== READ STATE =====
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    private LocalDateTime readAt;

    // ===== ACTION =====
    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "action_text", length = 100)
    private String actionText;

    // ===== STRIPE LINK =====
    private String stripeEventId;

    // ===== METADATA =====
    @Column(columnDefinition = "JSON")
    private String metadata;

    // ===== EXPIRY =====
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    // ===== ENUMS =====

    public enum NotificationType {
        subscription_active,
        subscription_inactive,
        payment_method_expired,
        payment_failed,
        payment_succeeded,
        subscription_canceled,
        subscription_renewed,
        plan_changed,
        addon_added,
        addon_removed,
        limit_warning,
        limit_exceeded,
        invoice_created,
        invoice_paid,
        invoice_failed
    }

    public enum Severity {
        info,
        warning,
        error,
        success
    }
}