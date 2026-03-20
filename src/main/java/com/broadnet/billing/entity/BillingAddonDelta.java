package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_addon_deltas",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_addon_delta_type_interval",
                        columnNames = {"addon_id", "delta_type", "billing_interval", "effective_from"}
                )
        },
        indexes = {
                @Index(name = "idx_addon_active", columnList = "addon_id, is_active, effective_from")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingAddonDelta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Relationship with BillingAddon
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "addon_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_addon_deltas_addon")
    )
    private BillingAddon addon;

    @Enumerated(EnumType.STRING)
    @Column(name = "delta_type", nullable = false)
    private DeltaType deltaType;

    @Column(name = "delta_value", nullable = false)
    private Integer deltaValue;

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

    public enum DeltaType {
        answers_per_period,
        kb_pages
    }

    public enum BillingInterval {
        month,
        year
    }
}