package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stripe_invoice", columnNames = "stripe_invoice_id")
        },
        indexes = {
                @Index(name = "idx_company_status", columnList = "company_id, status, invoice_date"),
                @Index(name = "idx_invoice_date", columnList = "invoice_date")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Company reference (high volume → keep as ID)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ===== STRIPE =====
    @Column(name = "stripe_invoice_id", nullable = false, unique = true)
    private String stripeInvoiceId;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    // ===== STATUS =====
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private InvoiceStatus status;

    // ===== AMOUNTS (in cents) =====
    @Column(name = "amount_due", nullable = false)
    private Integer amountDue;

    @Column(name = "amount_paid", nullable = false)
    @Builder.Default
    private Integer amountPaid = 0;

    @Column(name = "subtotal", nullable = false)
    private Integer subtotal;

    @Column(name = "tax_amount", nullable = false)
    @Builder.Default
    private Integer taxAmount = 0;

    @Column(name = "total", nullable = false)
    private Integer total;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "usd";

    // ===== DATES =====
    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    private LocalDateTime dueDate;
    private LocalDateTime paidAt;

    private LocalDateTime periodStart;
    private LocalDateTime periodEnd;

    // ===== STRIPE LINKS =====
    @Column(name = "hosted_invoice_url", length = 500)
    private String hostedInvoiceUrl;

    @Column(name = "invoice_pdf_url", length = 500)
    private String invoicePdfUrl;

    // ===== JSON FIELDS =====
    @Column(name = "line_items", nullable = false, columnDefinition = "JSON")
    private String lineItems;

    @Column(name = "metadata", columnDefinition = "JSON")
    private String metadata;

    // ===== AUDIT =====
    @Column(name = "created_at")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ===== ENUM =====
    public enum InvoiceStatus {
        draft,
        open,
        paid,
//        void    //due to keyword conflict implement in future
        uncollectible
        }
}