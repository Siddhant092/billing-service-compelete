package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_stripe_prices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stripe_price_id", columnNames = "stripe_price_id"),
                @UniqueConstraint(name = "uk_lookup_key", columnNames = "lookup_key")
        },
        indexes = {
                @Index(name = "idx_plan", columnList = "plan_id"),
                @Index(name = "idx_addon", columnList = "addon_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingStripePrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "stripe_price_id", nullable = false, unique = true)
    private String stripePriceId;

    @Column(name = "lookup_key", nullable = false, unique = true, length = 100)
    private String lookupKey;

    // 🔗 Plan (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "plan_id",
            foreignKey = @ForeignKey(name = "fk_stripe_price_plan")
    )
    private BillingPlan plan;

    // 🔗 Addon (nullable)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "addon_id",
            foreignKey = @ForeignKey(name = "fk_stripe_price_addon")
    )
    private BillingAddon addon;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    private BillingInterval billingInterval;

    @Column(name = "amount_cents", nullable = false)
    private Integer amountCents;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "usd";

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ===== ENUM =====
    public enum BillingInterval {
        month,
        year
    }
}