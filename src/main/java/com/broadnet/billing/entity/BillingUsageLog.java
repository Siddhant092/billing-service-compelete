package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_usage_logs",
        indexes = {
                @Index(name = "idx_company_type_created", columnList = "company_id, usage_type, created_at"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Company reference (simple FK)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ENUM mapping
    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false)
    private UsageType usageType;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 1;

    @Column(name = "before_count")
    private Integer beforeCount;

    @Column(name = "after_count")
    private Integer afterCount;

    @Column(name = "was_blocked")
    private Boolean wasBlocked;

    @Column(name = "block_reason")
    private String blockReason;

    // JSON metadata
    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    // ===== ENUM =====
    public enum UsageType {
        answer,
        kb_page_added,
        kb_page_updated,
        agent_created,
        user_created
    }
}