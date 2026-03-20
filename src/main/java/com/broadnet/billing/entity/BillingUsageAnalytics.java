package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_usage_analytics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_metric_period",
                        columnNames = {"company_id", "metric_type", "period_type", "period_start"}
                )
        },
        indexes = {
                @Index(name = "idx_company_period", columnList = "company_id, period_type, period_start"),
                @Index(name = "idx_metric_period", columnList = "metric_type, period_type, period_start")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingUsageAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Keep as ID (analytics table → high volume)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ===== METRIC =====
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type", nullable = false)
    private MetricType metricType;

    // ===== PERIOD =====
    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    // ===== DATA =====
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "limit_value")
    private Integer limitValue;

    // ===== AUDIT =====
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    // ===== ENUMS =====

    public enum MetricType {
        answers,
        kb_pages,
        agents,
        users
    }

    public enum PeriodType {
        hour,
        day,
        week,
        month
    }
}