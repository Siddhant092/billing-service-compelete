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
public class AvailableBoostResponse {

    @JsonProperty("addon_code")
    private String addonCode;

    @JsonProperty("addon_name")
    private String addonName;

    private String category;

    private String tier;

    @JsonProperty("delta_value")
    private int deltaValue;

    @JsonProperty("delta_type")
    private String deltaType;

    @JsonProperty("price_monthly")
    private int priceMonthly;

    @JsonProperty("price_annual")
    private int priceAnnual;

    @JsonProperty("already_active")
    private boolean alreadyActive;
}
