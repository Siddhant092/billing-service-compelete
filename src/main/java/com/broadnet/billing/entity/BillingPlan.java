package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "billing_plans",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_plan_code", columnNames = "plan_code")
        },
        indexes = {
                @Index(name = "idx_active", columnList = "is_active")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_code", nullable = false, length = 50, unique = true)
    private String planCode;

    @Column(name = "plan_name", nullable = false, length = 255)
    private String planName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_enterprise", nullable = false)
    @Builder.Default
    private Boolean isEnterprise = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "support_tier")
    @Builder.Default
    private SupportTier supportTier = SupportTier.basic;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    // 🔗 Relationship with BillingPlanLimit
    @OneToMany(
            mappedBy = "plan",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<BillingPlanLimit> limits;

    public enum SupportTier {
        basic,
        standard,
        priority,
        dedicated
    }
}