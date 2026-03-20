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
public class EnterpriseSummaryResponse {

    @JsonProperty("billing_mode")
    private String billingMode;

    @JsonProperty("pricing_tier")
    private String pricingTier;

    @JsonProperty("annual_commitment")
    private Long annualCommitment;

    @JsonProperty("monthly_minimum")
    private Long monthlyMinimum;

    @JsonProperty("contract_reference")
    private String contractReference;

    private RatesDto rates;

    @JsonProperty("current_period")
    private CurrentPeriodDto currentPeriod;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RatesDto {
        @JsonProperty("answers_rate_cents")
        private int answersRateCents;

        @JsonProperty("kb_pages_rate_cents")
        private int kbPagesRateCents;

        @JsonProperty("agents_rate_cents")
        private int agentsRateCents;

        @JsonProperty("users_rate_cents")
        private int usersRateCents;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CurrentPeriodDto {
        private LocalDateTime start;
        private LocalDateTime end;
        private UsageDto usage;
        @JsonProperty("calculated_amount")
        private Long calculatedAmount;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @Builder
        public static class UsageDto {
            private Long answers;
            @JsonProperty("kb_pages")
            private Long kbPages;
            private Integer agents;
            private Integer users;
        }
    }
}
