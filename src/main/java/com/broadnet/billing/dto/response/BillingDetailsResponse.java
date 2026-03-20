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
public class BillingDetailsResponse {

    @JsonProperty("billing_address")
    private AddressDto billingAddress;

    @JsonProperty("payment_method")
    private PaymentMethodDetailsDto paymentMethod;

    @JsonProperty("tax_id")
    private String taxId;

    @JsonProperty("company_name")
    private String companyName;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AddressDto {
        private String street;
        private String city;
        private String state;

        @JsonProperty("postal_code")
        private String postalCode;

        private String country;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PaymentMethodDetailsDto {
        private String type;
        private String brand;

        @JsonProperty("last4")
        private String last4;

        @JsonProperty("exp_month")
        private Integer expMonth;

        @JsonProperty("exp_year")
        private Integer expYear;

        @JsonProperty("is_default")
        private Boolean isDefault;

        @JsonProperty("is_expired")
        private Boolean isExpired;
    }
}