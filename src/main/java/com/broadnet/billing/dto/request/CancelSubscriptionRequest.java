package com.broadnet.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CancelSubscriptionRequest {

    @JsonProperty("cancel_at_period_end")
    private boolean cancelAtPeriodEnd = false;  // false = immediate

    @Size(max = 1000)
    private String reason;  // optional feedback
}