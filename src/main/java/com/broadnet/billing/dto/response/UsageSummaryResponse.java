package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageSummaryResponse {

    private PeriodDto period;

    private SummaryDto summary;

    private TrendsDto trends;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PeriodDto {
        @JsonProperty("start_date")
        private LocalDateTime startDate;

        @JsonProperty("end_date")
        private LocalDateTime endDate;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SummaryDto {
        private UsageBreakdown answers;

        @JsonProperty("kb_pages")
        private UsageBreakdown kbPages;

        private UsageBreakdown agents;
        private UsageBreakdown users;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class UsageBreakdown {
            @JsonProperty("total_used")
            private Integer totalUsed;

            @JsonProperty("average_per_day")
            private Double averagePerDay;

            @JsonProperty("peak_day")
            private Integer peakDay;

            @JsonProperty("blocked_count")
            private Integer blockedCount;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TrendsDto {
        private List<DailyTrendDto> daily;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        @JsonInclude(JsonInclude.Include.NON_NULL)
        public static class DailyTrendDto {
            private LocalDate date;

            @JsonProperty("answers_used")
            private Integer answersUsed;

            @JsonProperty("kb_pages_added")
            private Integer kbPagesAdded;

            @JsonProperty("agents_created")
            private Integer agentsCreated;

            @JsonProperty("users_added")
            private Integer usersAdded;
        }
    }
}