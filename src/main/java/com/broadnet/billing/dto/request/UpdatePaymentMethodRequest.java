package com.broadnet.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePaymentMethodRequest {

    @NotBlank(message = "Payment method token is required")
    @JsonProperty("payment_method_token")
    private String paymentMethodToken;

    @JsonProperty("set_as_default")
    @Builder.Default
    private boolean setAsDefault = true;
}