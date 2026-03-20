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
public class BillingSnapshotResponse {

    @JsonProperty("subscription_status")
    private String subscriptionStatus;  // active, canceled, past_due, etc

    @JsonProperty("payment_method")
    private PaymentMethodDto paymentMethod;

    @JsonProperty("next_invoice")
    private InvoicePreviewDto nextInvoice;

    @JsonProperty("payment_failure_date")
    private LocalDateTime paymentFailureDate;

    @JsonProperty("service_restricted")
    private boolean serviceRestricted;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentMethodDto {
        private String type;
        private String brand;
        @JsonProperty("last4")
        private String last4;
        @JsonProperty("exp_month")
        private Integer expMonth;
        @JsonProperty("exp_year")
        private Integer expYear;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InvoicePreviewDto {
        private LocalDateTime date;
        private int amount;
        private String currency;
    }
}
