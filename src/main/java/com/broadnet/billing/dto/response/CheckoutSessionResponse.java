package com.broadnet.billing.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RESPONSE DTOs FOR BILLING API
 * All response objects returned by controllers
 */

// ===== CHECKOUT RESPONSES =====

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CheckoutSessionResponse {

    @JsonProperty("checkout_session_id")
    private String checkoutSessionId;

    private String url;  // Stripe checkout URL
}

// ===== USAGE ENFORCEMENT RESPONSES =====


// ===== BILLING DASHBOARD RESPONSES =====

// ===== INVOICE RESPONSES =====

// ===== USAGE ANALYTICS RESPONSES =====

// ===== BILLING DETAILS RESPONSES =====

// ===== ENTERPRISE RESPONSES =====

// ===== ADMIN RESPONSES =====

