package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BoostPurchaseResponse {

    private boolean success;

    private String message;

    @JsonProperty("addon_code")
    private String addonCode;

    @JsonProperty("purchase_date")
    private LocalDateTime purchaseDate;

    @JsonProperty("new_limits")
    private Map<String, Integer> newLimits;
}
