package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_plan_limits",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_plan_limit_type_interval",
                        columnNames = {"plan_id", "limit_type", "billing_interval", "effective_from"}
                )
        },
        indexes = {
                @Index(name = "idx_plan_active", columnList = "plan_id, is_active, effective_from")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlanLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Relationship with BillingPlan
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_plan_limits_plan"))
    private BillingPlan plan;

    // ENUM: answers_per_period, kb_pages, agents, users
    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false)
    private LimitType limitType;

    @Column(name = "limit_value", nullable = false)
    private Integer limitValue;

    // ENUM: month, year
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval", nullable = false)
    @Builder.Default
    private BillingInterval billingInterval = BillingInterval.month;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "effective_from", insertable = false, updatable = false)
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 🔒 Optimistic Locking
    @Version
    @Column(name = "version", nullable = false)
    private Integer version;

    // ===== ENUMS =====
    public enum LimitType {
        answers_per_period,
        kb_pages,
        agents,
        users
    }

    public enum BillingInterval {
        month,
        year
    }
}