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
public class SubscriptionResponse {

    private Boolean success;
    private String message;

    @JsonProperty("active_plan_code")
    private String activePlanCode;

    @JsonProperty("previous_plan_code")
    private String previousPlanCode;

    @JsonProperty("effective_date")
    private LocalDateTime effectiveDate;

    private String status;

    @JsonProperty("cancellation_type")
    private String cancellationType;

    @JsonProperty("period_end_date")
    private LocalDateTime periodEndDate;
}