package com.broadnet.billing.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

/**
 * REQUEST DTOs FOR BILLING API
 * All request objects with validation constraints
 */

// ===== CHECKOUT ENDPOINTS =====

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCheckoutSessionRequest {

    @NotBlank(message = "plan_code is required")
    @Size(min = 2, max = 50)
    @JsonProperty("plan_code")
    private String planCode;

    @NotBlank(message = "billing_interval is required")
    @Pattern(regexp = "month|year")
    @JsonProperty("billing_interval")
    private String billingInterval;

    @NotBlank(message = "success_url is required")
    @URL(message = "success_url must be valid URL")
    @JsonProperty("success_url")
    private String successUrl;

    @NotBlank(message = "cancel_url is required")
    @URL(message = "cancel_url must be valid URL")
    @JsonProperty("cancel_url")
    private String cancelUrl;
}

