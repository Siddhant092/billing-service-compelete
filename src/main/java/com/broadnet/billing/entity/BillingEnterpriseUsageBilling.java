package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_enterprise_usage_billing",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_company_period",
                        columnNames = {"company_id", "billing_period_start", "billing_period_end"}
                )
        },
        indexes = {
                @Index(name = "idx_billing_status", columnList = "billing_status, billing_period_end"),
                @Index(name = "idx_invoice", columnList = "invoice_id"),
                @Index(name = "idx_stripe_invoice", columnList = "stripe_invoice_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEnterpriseUsageBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    private LocalDateTime billingPeriodStart;
    private LocalDateTime billingPeriodEnd;

    @Enumerated(EnumType.STRING)
    private BillingStatus billingStatus;

    // ===== USAGE =====
    private Integer answersUsed;
    private Integer kbPagesUsed;
    private Integer agentsUsed;
    private Integer usersUsed;

    // ===== RATES =====
    private Integer answersRateCents;
    private Integer kbPagesRateCents;
    private Integer agentsRateCents;
    private Integer usersRateCents;

    // ===== AMOUNTS =====
    private Integer answersAmountCents;
    private Integer kbPagesAmountCents;
    private Integer agentsAmountCents;
    private Integer usersAmountCents;

    private Integer subtotalCents;
    private Integer taxAmountCents;
    private Integer totalCents;

    // ===== INVOICE =====
    private String stripeInvoiceId;
    private Long invoiceId;
    private LocalDateTime invoicedAt;

    // ===== META =====
    @Column(columnDefinition = "TEXT")
    private String calculationNotes;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum BillingStatus {
        pending,
        calculated,
        invoiced,
        paid
    }
}