package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_payment_methods",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stripe_pm", columnNames = "stripe_payment_method_id")
        },
        indexes = {
                @Index(name = "idx_company_default", columnList = "company_id, is_default"),
                @Index(name = "idx_expired", columnList = "is_expired, card_exp_year, card_exp_month")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingPaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔗 Keep as ID (high-volume, frequent access)
    @Column(name = "company_id", nullable = false)
    private Long companyId;

    // ===== STRIPE =====
    @Column(name = "stripe_payment_method_id", nullable = false, unique = true)
    private String stripePaymentMethodId;

    // ===== TYPE =====
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethodType type;

    // ===== DEFAULT FLAG =====
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    // ===== CARD DETAILS =====
    private String cardBrand;
    private String cardLast4;
    private Integer cardExpMonth;
    private Integer cardExpYear;

    @Column(name = "is_expired", nullable = false)
    @Builder.Default
    private Boolean isExpired = false;

    // ===== BILLING DETAILS =====
    @Column(name = "billing_details", columnDefinition = "JSON")
    private String billingDetails;

    // ===== AUDIT =====
    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ===== ENUM =====
    public enum PaymentMethodType {
        card,
        bank_account,
        sepa_debit
    }

}