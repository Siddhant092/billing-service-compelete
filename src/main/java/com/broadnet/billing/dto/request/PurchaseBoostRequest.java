package com.broadnet.billing.dto.request;

// ===== BILLING DASHBOARD ENDPOINTS =====

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseBoostRequest {

    @NotBlank(message = "addon_code is required")
    @Size(min = 2, max = 50)
    @JsonProperty("addon_code")
    private String addonCode;

    @NotBlank(message = "billing_interval is required")
    @Pattern(regexp = "month|year")
    @JsonProperty("billing_interval")
    private String billingInterval;
}