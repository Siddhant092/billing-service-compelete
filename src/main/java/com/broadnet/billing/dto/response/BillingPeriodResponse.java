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
public class BillingPeriodResponse {

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;

    private String status;

    private UsageDto usage;

    private AmountsDto amounts;

    private Integer subtotal;

    private Integer tax;

    private Integer total;

    @JsonProperty("invoice_id")
    private Long invoiceId;

    @JsonProperty("stripe_invoice_id")
    private String stripeInvoiceId;

    private Boolean paid;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UsageDto {
        private Integer answers;

        @JsonProperty("kb_pages")
        private Integer kbPages;

        private Integer agents;
        private Integer users;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AmountsDto {
        private Integer answers;

        @JsonProperty("kb_pages")
        private Integer kbPages;

        private Integer agents;
        private Integer users;
    }
}