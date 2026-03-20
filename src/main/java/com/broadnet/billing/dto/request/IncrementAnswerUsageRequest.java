package com.broadnet.billing.dto.request;

// ===== USAGE ENFORCEMENT ENDPOINTS =====

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
//@NoArgsConstructor
//@AllArgsConstructor
@Builder
public class IncrementAnswerUsageRequest {
    // company_id comes from header, not body
    // count = 1 always (one answer = one increment)
}
