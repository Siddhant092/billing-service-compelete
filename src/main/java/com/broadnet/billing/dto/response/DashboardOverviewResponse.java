package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashboardOverviewResponse {

    @JsonProperty("current_plan")
    private CurrentPlanResponse currentPlan;

    @JsonProperty("billing_snapshot")
    private BillingSnapshotResponse billingSnapshot;

    @JsonProperty("usage_metrics")
    private UsageMetricsResponse usageMetrics;

    private List<BillingNotificationResponse> notifications;

    @JsonProperty("available_boosts")
    private List<AvailableBoostResponse> availableBoosts;
}
