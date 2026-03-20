package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_enterprise_pricing",
        indexes = {
                @Index(name = "idx_company_active", columnList = "company_id, is_active, effective_from"),
                @Index(name = "idx_effective_dates", columnList = "effective_from, effective_to")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEnterprisePricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    private PricingTier pricingTier;

    // ===== RATES =====
    private Integer answersRateCents;
    private Integer kbPagesRateCents;
    private Integer agentsRateCents;
    private Integer usersRateCents;

    // ===== VOLUME DISCOUNTS =====
    @Column(columnDefinition = "JSON")
    private String answersVolumeDiscountTiers;

    @Column(columnDefinition = "JSON")
    private String kbPagesVolumeDiscountTiers;

    // ===== COMMITMENTS =====
    private Integer minimumMonthlyCommitmentCents;
    private Integer minimumAnswersCommitment;

    // ===== EFFECTIVE =====
    private LocalDateTime effectiveFrom;
    private LocalDateTime effectiveTo;

    @Builder.Default
    private Boolean isActive = true;

    // ===== APPROVAL =====
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private String contractReference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum PricingTier {
        standard,
        custom,
        negotiated
    }
}