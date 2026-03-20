package com.broadnet.billing.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetEnterprisePricingRequest {

    @NotNull(message = "company_id is required")
    @JsonProperty("company_id")
    private Long companyId;

    @NotBlank(message = "pricing_tier is required")
    @JsonProperty("pricing_tier")
    private String pricingTier;  // negotiated, volume_discount, etc

    @NotNull(message = "answers_rate_cents is required")
    @Min(0)
    @JsonProperty("answers_rate_cents")
    private Integer answersRateCents;

    @NotNull(message = "kb_pages_rate_cents is required")
    @Min(0)
    @JsonProperty("kb_pages_rate_cents")
    private Integer kbPagesRateCents;

    @NotNull(message = "agents_rate_cents is required")
    @Min(0)
    @JsonProperty("agents_rate_cents")
    private Integer agentsRateCents;

    @NotNull(message = "users_rate_cents is required")
    @Min(0)
    @JsonProperty("users_rate_cents")
    private Integer usersRateCents;

    @Min(0)
    @JsonProperty("minimum_monthly_commitment_cents")
    private Integer minimumMonthlyCommitmentCents;

    @Min(0)
    @JsonProperty("minimum_answers_commitment")
    private Long minimumAnswersCommitment;

    @JsonProperty("answers_volume_discount_tiers")
    private java.util.List<VolumeTier> answersVolumeTiers;

    @JsonProperty("kb_pages_volume_discount_tiers")
    private java.util.List<VolumeTier> kbPagesVolumeTiers;

    @NotNull(message = "effective_from is required")
    @JsonProperty("effective_from")
    private LocalDateTime effectiveFrom;

    @Size(max = 100)
    @JsonProperty("contract_reference")
    private String contractReference;

    @Size(max = 1000)
    private String notes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VolumeTier {
        @Min(0)
        @JsonProperty("minimum_volume")
        private Long minimumVolume;

        @Min(0)
        @JsonProperty("discount_percentage")
        private Integer discountPercentage;
    }
}

