package com.broadnet.billing.dto.request;

// ===== SUBSCRIPTION ENDPOINTS =====

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePlanRequest {

    @NotBlank(message = "plan_code is required")
    @Size(min = 2, max = 50)
    @JsonProperty("plan_code")
    private String planCode;

    @NotBlank(message = "billing_interval is required")
    @Pattern(regexp = "month|year")
    @JsonProperty("billing_interval")
    private String billingInterval;
}