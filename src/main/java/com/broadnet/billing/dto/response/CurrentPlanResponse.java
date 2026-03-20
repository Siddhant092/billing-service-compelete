package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CurrentPlanResponse {

    @JsonProperty("plan_code")
    private String planCode;

    @JsonProperty("plan_name")
    private String planName;

    @JsonProperty("support_tier")
    private String supportTier;

    @JsonProperty("active_addons")
    private List<String> activeAddons;

    private Map<String, Integer> limits;

    @JsonProperty("billing_interval")
    private String billingInterval;

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;
}
