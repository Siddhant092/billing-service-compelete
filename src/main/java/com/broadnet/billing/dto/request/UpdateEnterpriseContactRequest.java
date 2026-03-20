package com.broadnet.billing.dto.request;


import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateEnterpriseContactRequest {

    @Pattern(regexp = "pending|contacted|in_progress|qualified|closed")
    private String status;

    @JsonProperty("assigned_to")
    private Long assignedTo;

    @Pattern(regexp = "won|lost")
    private String outcome;

    @Size(max = 2000)
    private String notes;
}
