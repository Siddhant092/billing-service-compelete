package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AvailablePlanResponse {

    private Long id;

    @JsonProperty("plan_code")
    private String planCode;

    @JsonProperty("plan_name")
    private String planName;

    private String description;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_enterprise")
    private Boolean isEnterprise;

    @JsonProperty("support_tier")
    private String supportTier;
}