package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "company_billing",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_company_id", columnNames = "company_id"),
                @UniqueConstraint(name = "uk_stripe_customer", columnNames = "stripe_customer_id"),
                @UniqueConstraint(name = "uk_stripe_subscription", columnNames = "stripe_subscription_id")
        },
        indexes = {
                @Index(name = "idx_subscription_status", columnList = "subscription_status"),
                @Index(name = "idx_service_restricted", columnList = "service_restricted_at"),
                @Index(name = "idx_payment_failure", columnList = "payment_failure_date"),
                @Index(name = "idx_answers_reset_day", columnList = "billing_interval, answers_reset_day")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Company (1:1)
    @Column(name = "company_id", nullable = false, unique = true)
    private Long companyId;

    // ===== STRIPE =====
    @Column(name = "stripe_customer_id", nullable = false)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "stripe_schedule_id")
    private String stripeScheduleId;

    // ===== SUBSCRIPTION STATE =====
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_status")
    private SubscriptionStatus subscriptionStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_interval")
    private BillingInterval billingInterval;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    // ===== CANCELLATION =====
    private Boolean cancelAtPeriodEnd;
    private LocalDateTime cancelAt;
    private LocalDateTime canceledAt;

    // ===== ACTIVE PLAN =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_plan_id",
            foreignKey = @ForeignKey(name = "fk_company_billing_plan"))
    private BillingPlan activePlan;

    @Column(name = "active_plan_code")
    private String activePlanCode;

    // ===== EFFECTIVE LIMITS =====
    @Builder.Default
    private Integer effectiveAnswersLimit = 0;

    @Builder.Default
    private Integer effectiveKbPagesLimit = 0;

    @Builder.Default
    private Integer effectiveAgentsLimit = 0;

    @Builder.Default
    private Integer effectiveUsersLimit = 0;

    // ===== ADDONS (JSON) =====
    @Column(name = "active_addon_codes", columnDefinition = "JSON")
    private String activeAddonCodes;

    // ===== PENDING CHANGES =====
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pending_plan_id",
            foreignKey = @ForeignKey(name = "fk_company_billing_pending_plan"))
    private BillingPlan pendingPlan;

    @Column(name = "pending_plan_code")
    private String pendingPlanCode;

    @Column(name = "pending_addon_codes", columnDefinition = "JSON")
    private String pendingAddonCodes;

    private LocalDateTime pendingEffectiveDate;

    // ===== USAGE =====
    @Builder.Default
    private Integer answersUsedInPeriod = 0;

    private LocalDateTime answersPeriodStart;

    private Integer answersResetDay;

    @Builder.Default
    private Integer kbPagesTotal = 0;

    @Builder.Default
    private Integer agentsTotal = 0;

    @Builder.Default
    private Integer usersTotal = 0;

    // ===== GRACE PERIOD =====
    private LocalDateTime paymentFailureDate;
    private LocalDateTime serviceRestrictedAt;

    @Enumerated(EnumType.STRING)
    private RestrictionReason restrictionReason;

    // ===== BLOCK FLAGS =====
    @Builder.Default
    private Boolean answersBlocked = false;

    // ===== BILLING MODE =====
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_mode", nullable = false)
    @Builder.Default
    private BillingMode billingMode = BillingMode.prepaid;

    private Long enterprisePricingId;

    private LocalDateTime currentBillingPeriodStart;
    private LocalDateTime currentBillingPeriodEnd;

    // ===== METADATA =====
    private LocalDateTime lastWebhookAt;
    private LocalDateTime lastSyncAt;

    @Version
    private Integer version;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ===== ENUMS =====

    public enum SubscriptionStatus {
        active, past_due, canceled, trialing, unpaid, incomplete, incomplete_expired
    }

    public enum BillingInterval {
        month, year
    }

    public enum RestrictionReason {
        payment_failed, canceled, admin
    }

    public enum BillingMode {
        prepaid, postpaid
    }
}