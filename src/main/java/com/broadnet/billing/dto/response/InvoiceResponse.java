package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class InvoiceResponse {

    private Long id;

    @JsonProperty("invoice_number")
    private String invoiceNumber;

    @JsonProperty("stripe_invoice_id")
    private String stripeInvoiceId;

    private String status;

    @JsonProperty("amount_due")
    private int amountDue;

    @JsonProperty("amount_paid")
    private int amountPaid;

    private int total;

    private String currency;

    @JsonProperty("invoice_date")
    private LocalDateTime invoiceDate;

    @JsonProperty("due_date")
    private LocalDateTime dueDate;

    @JsonProperty("paid_at")
    private LocalDateTime paidAt;

    @JsonProperty("period_start")
    private LocalDateTime periodStart;

    @JsonProperty("period_end")
    private LocalDateTime periodEnd;

    @JsonProperty("hosted_invoice_url")
    private String hostedInvoiceUrl;

    @JsonProperty("invoice_pdf_url")
    private String invoicePdfUrl;

    @JsonProperty("line_items")
    private List<LineItemDto> lineItems;

    private int subtotal;

    @JsonProperty("tax_amount")
    private int taxAmount;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LineItemDto {
        private String description;
        private int quantity;
        @JsonProperty("unit_amount")
        private int unitAmount;
        private int amount;
    }
}
