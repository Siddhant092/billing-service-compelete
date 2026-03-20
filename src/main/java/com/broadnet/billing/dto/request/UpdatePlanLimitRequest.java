package com.broadnet.billing.dto.request;


// ===== ADMIN ENDPOINTS =====

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePlanLimitRequest {

    @NotBlank(message = "limit_type is required")
    @Pattern(regexp = "answers_per_period|kb_pages|agents|users")
    @JsonProperty("limit_type")
    private String limitType;

    @NotNull(message = "limit_value is required")
    @Min(0)
    @JsonProperty("limit_value")
    private Integer limitValue;

    @NotBlank(message = "billing_interval is required")
    @Pattern(regexp = "month|year")
    @JsonProperty("billing_interval")
    private String billingInterval;

    @NotNull(message = "effective_from is required")
    @JsonProperty("effective_from")
    private LocalDateTime effectiveFrom;
}

