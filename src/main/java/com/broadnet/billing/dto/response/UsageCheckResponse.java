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
public class UsageCheckResponse {

    private Boolean success;
    private Boolean blocked;
    private Boolean allowed;

    @JsonProperty("current_usage")
    private Integer currentUsage;

    private Integer limit;

    private Integer remaining;

    @JsonProperty("usage_percentage")
    private Double usagePercentage;

    private String error;

    private String message;

    @JsonProperty("warning_level")
    private String warningLevel;  // ok, warning, critical

    @JsonProperty("reset_at")
    private LocalDateTime resetAt;
}