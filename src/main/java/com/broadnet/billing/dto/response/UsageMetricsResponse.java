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
public class UsageMetricsResponse {

    private MetricBreakdown answers;

    @JsonProperty("kb_pages")
    private MetricBreakdown kbPages;

    private MetricBreakdown agents;

    private MetricBreakdown users;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MetricBreakdown {
        private int used;
        private int limit;
        private int percentage;
        @JsonProperty("warning_level")
        private String warningLevel;
        @JsonProperty("reset_at")
        private LocalDateTime resetAt;
    }
}
