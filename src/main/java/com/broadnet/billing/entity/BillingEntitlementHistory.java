package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_entitlement_history",
        indexes = {
                @Index(name = "idx_company_created", columnList = "company_id, created_at"),
                @Index(name = "idx_stripe_event", columnList = "stripe_event_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEntitlementHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Company reference (high-volume → keep as ID)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ===== CHANGE TYPE =====
    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false)
    private ChangeType changeType;

    // ===== PLAN =====
    private String oldPlanCode;
    private String newPlanCode;

    // ===== ADDONS (JSON) =====
    @Column(name = "old_addon_codes", columnDefinition = "JSON")
    private String oldAddonCodes;

    @Column(name = "new_addon_codes", columnDefinition = "JSON")
    private String newAddonCodes;

    // ===== LIMITS =====
    private Integer oldAnswersLimit;
    private Integer newAnswersLimit;

    private Integer oldKbPagesLimit;
    private Integer newKbPagesLimit;

    private Integer oldAgentsLimit;
    private Integer newAgentsLimit;

    private Integer oldUsersLimit;
    private Integer newUsersLimit;

    // ===== SOURCE =====
    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false)
    private TriggeredBy triggeredBy;

    private String stripeEventId;

    private LocalDateTime effectiveDate;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    // ===== ENUMS =====

    public enum ChangeType {
        plan_change,
        addon_added,
        addon_removed,
        addon_upgraded,
        addon_downgraded,
        limit_update
    }

    public enum TriggeredBy {
        webhook,
        admin,
        api
    }
}