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
public class UsageStatus {

    @JsonProperty("answers_status")
    private MetricStatus answersStatus;

    @JsonProperty("kb_pages_status")
    private MetricStatus kbPagesStatus;

    @JsonProperty("agents_status")
    private MetricStatus agentsStatus;

    @JsonProperty("users_status")
    private MetricStatus usersStatus;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MetricStatus {
        private Integer used;
        private Integer limit;
        private Double percentage;

        @JsonProperty("warning_level")
        private String warningLevel;

        @JsonProperty("reset_at")
        private LocalDateTime resetAt;
    }
}