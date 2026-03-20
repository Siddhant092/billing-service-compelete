package com.broadnet.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateEnterpriseContactRequest {

    @NotBlank(message = "Contact type is required")
    @JsonProperty("contact_type")
    @Pattern(regexp = "enterprise_inquiry|pricing_request|custom_plan_request|support_request",
            message = "Contact type must be one of: enterprise_inquiry, pricing_request, custom_plan_request, support_request")
    private String contactType;

    @NotBlank(message = "Name is required")
    @Size(max = 255, message = "Name must not exceed 255 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;

    @JsonProperty("job_title")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    private String jobTitle;

    @NotBlank(message = "Company name is required")
    @JsonProperty("company_name")
    @Size(max = 255, message = "Company name must not exceed 255 characters")
    private String companyName;

    @NotBlank(message = "Company size is required")
    @JsonProperty("company_size")
    @Pattern(regexp = "_1_10|_11_50|_51_200|_201_500|_501_1000|_1000_plus",
            message = "Company size must be one of: _1_10, _11_50, _51_200, _201_500, _501_1000, _1000_plus")
    private String companySize;

    @Size(max = 2000, message = "Message must not exceed 2000 characters")
    private String message;

    @Valid
    @JsonProperty("estimated_usage")
    private EstimatedUsage estimatedUsage;

    @JsonProperty("budget_range")
    @Size(max = 255, message = "Budget range must not exceed 255 characters")
    private String budgetRange;

    @NotBlank(message = "Preferred contact method is required")
    @JsonProperty("preferred_contact_method")
    @Pattern(regexp = "email|phone|video_call",
            message = "Preferred contact method must be one of: email, phone, video_call")
    private String preferredContactMethod;

    @JsonProperty("preferred_contact_time")
    @Size(max = 255, message = "Preferred contact time must not exceed 255 characters")
    private String preferredContactTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EstimatedUsage {
        @JsonProperty("answers_per_month")
        @Min(value = 0, message = "Answers per month must be non-negative")
        private Integer answersPerMonth;

        @JsonProperty("kb_pages")
        @Min(value = 0, message = "KB pages must be non-negative")
        private Integer kbPages;

        @Min(value = 0, message = "Agents must be non-negative")
        private Integer agents;

        @Min(value = 0, message = "Users must be non-negative")
        private Integer users;
    }
}