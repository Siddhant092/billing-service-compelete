package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnterpriseContactAdminResponse {

    private Long id;

    @JsonProperty("company_id")
    private Long companyId;

    @JsonProperty("contact_type")
    private String contactType;

    private String name;

    private String email;

    private String phone;

    @JsonProperty("job_title")
    private String jobTitle;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("company_size")
    private String companySize;

    private String status;

    @JsonProperty("assigned_to")
    private Long assignedTo;

    @JsonProperty("assigned_at")
    private LocalDateTime assignedAt;

    @JsonProperty("first_contacted_at")
    private LocalDateTime firstContactedAt;

    private String message;

    private EstimatedUsageDto estimatedUsage;

    @JsonProperty("budget_range")
    private String budgetRange;

    private String outcome;

    private String notes;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstimatedUsageDto {
        @JsonProperty("answers_per_month")
        private Long answersPerMonth;

        @JsonProperty("kb_pages")
        private Long kbPages;

        private Integer agents;

        private Integer users;
    }
}
