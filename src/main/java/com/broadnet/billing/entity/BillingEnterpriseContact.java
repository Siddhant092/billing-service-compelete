package com.broadnet.billing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "billing_enterprise_contacts",
        indexes = {
                @Index(name = "idx_company", columnList = "company_id"),
                @Index(name = "idx_status", columnList = "status, created_at"),
                @Index(name = "idx_assigned", columnList = "assigned_to, status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillingEnterpriseContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long companyId;

    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    // ===== CONTACT INFO =====
    private String name;
    private String email;
    private String phone;
    private String jobTitle;

    private String companyName;

    @Enumerated(EnumType.STRING)
    private CompanySize companySize;

    // ===== REQUEST =====
    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "JSON")
    private String estimatedUsage;

    private String budgetRange;

    @Enumerated(EnumType.STRING)
    private ContactMethod preferredContactMethod;

    private String preferredContactTime;

    // ===== STATUS =====
    @Enumerated(EnumType.STRING)
    private Status status;

    private Long assignedTo;
    private LocalDateTime assignedAt;
    private LocalDateTime firstContactedAt;
    private LocalDateTime closedAt;

    @Enumerated(EnumType.STRING)
    private Outcome outcome;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "JSON")
    private String metadata;

    @Column(name = "created_at", updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // ===== ENUMS =====

    public enum ContactType {
        enterprise_inquiry,
        pricing_request,
        custom_plan_request,
        support_request
    }

    public enum CompanySize {
        _1_10, _11_50, _51_200, _201_500, _501_1000, _1000_plus
    }

    public enum ContactMethod {
        email,
        phone,
        video_call
    }

    public enum Status {
        pending,
        contacted,
        in_progress,
        qualified,
        closed,
        rejected
    }

    public enum Outcome {
        signed,
        declined,
        no_response,
        not_qualified
    }
}