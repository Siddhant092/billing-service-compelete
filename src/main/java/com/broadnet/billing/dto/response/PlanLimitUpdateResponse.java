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
public class PlanLimitUpdateResponse {

    private boolean success;

    private String message;

    @JsonProperty("affected_companies")
    private int affectedCompanies;

    @JsonProperty("background_job_id")
    private String backgroundJobId;
}
