package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_webhook_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stripe_event_id", columnNames = "stripe_event_id")
        },
        indexes = {
                @Index(name = "idx_processed", columnList = "processed, created_at"),
                @Index(name = "idx_subscription", columnList = "stripe_subscription_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Stripe event id (idempotency key)
    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    // Full webhook payload
    @Column(name = "payload", nullable = false, columnDefinition = "JSON")
    private String payload;

    // Processing status
    @Column(name = "processed", nullable = false)
    @Builder.Default
    private Boolean processed = false;

    private LocalDateTime processedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;
}