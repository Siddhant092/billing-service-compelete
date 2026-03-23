package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.IncrementAnswerUsageRequest;
import com.broadnet.billing.dto.request.CheckKbPageLimitRequest;
import com.broadnet.billing.dto.response.UsageCheckResponse;
import com.broadnet.billing.service.UsageEnforcementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Usage Enforcement API Controller
 * Real-time usage checking and blocking against plan/addon limits
 *
 * Critical: These endpoints BLOCK users if limits exceeded
 * Performance: Must be sub-100ms for good UX
 *
 * Security: Requires authenticated user
 * Rate Limiting: Per company, not per IP
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/usage")
@RequiredArgsConstructor
public class UsageEnforcementController {

    private final UsageEnforcementService usageService;

    /**
     * Increment answer usage and check against limits
     * ATOMIC OPERATION - Prevents double-counting
     *
     * POST /api/billing/usage/increment-answer
     *
     * @param request IncrementAnswerUsageRequest with company_id (from auth)
     * @return UsageCheckResponse with success status and current usage
     *
     * Response on SUCCESS:
     * {
     *   "success": true,
     *   "answers_used": 1500,
     *   "answers_limit": 8000,
     *   "remaining": 6500,
     *   "blocked": false
     * }
     *
     * Response on BLOCKED:
     * {
     *   "success": false,
     *   "error": "ANSWER_LIMIT_EXCEEDED",
     *   "answers_used": 8000,
     *   "answers_limit": 8000,
     *   "blocked": true,
     *   "message": "You've reached your answer limit. Upgrade plan or add a boost."
     * }
     *
     * Flow:
     * 1. Get current CompanyBilling with PESSIMISTIC LOCK
     * 2. Get effective limits (BillingPlanLimit + BillingAddonDelta)
     * 3. Check: answersUsedInPeriod < effectiveAnswersLimit?
     * 4a. YES: Increment usage, release lock, return success
     * 4b. NO: Set blocked flag, release lock, return blocked error
     * 5. Log usage event to BillingUsageLog
     * 6. Create notification if approaching limit (80%+)
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 404: Company billing not found
     * - 409: Concurrent modification (retry)
     * - 500: Database error
     *
     * Performance Target: < 50ms (pessimistic lock is necessary here)
     */
    @PostMapping("/increment-answer")
    public ResponseEntity<UsageCheckResponse> incrementAnswerUsage(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody IncrementAnswerUsageRequest request,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Incrementing answer usage for company: {}", companyId);

        try {
            UsageCheckResponse response = usageService.checkAndEnforceAnswersUsage(
                    userId,
                    companyId,
                    1  // Each answer = 1 unit
            );

            if (response.getBlocked()) {
                log.warn("Answer usage blocked for company: {}, used: {}/{}",
                        companyId, response.getCurrentUsage(), response.getLimit());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            log.debug("Answer usage incremented for company: {}, now at: {}/{}",
                    companyId, response.getCurrentUsage(), response.getLimit());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking answer usage for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Check if KB page creation is allowed within limit
     * READ-ONLY operation (no increment)
     *
     * POST /api/billing/usage/check-kb-page
     *
     * @param request CheckKbPageLimitRequest
     * @return UsageCheckResponse with current KB page usage
     *
     * Response:
     * {
     *   "allowed": true,
     *   "kb_pages_total": 950,
     *   "kb_pages_limit": 1000,
     *   "remaining": 50
     * }
     *
     * Flow:
     * 1. Get CompanyBilling (READ only, no lock)
     * 2. Get effective KB pages limit (BillingPlanLimit + BillingAddonDelta)
     * 3. Check: kbPagesTotal < limit?
     * 4. Return status (no state change)
     *
     * Note: Actual increment happens in KbPageService when KB page is created
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 404: Company billing not found
     * - 500: Database error
     *
     * Performance Target: < 20ms (read-only)
     */
    @PostMapping("/check-kb-page")
    public ResponseEntity<UsageCheckResponse> checkKbPageLimit(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CheckKbPageLimitRequest request,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Checking KB page limit for company: {}", companyId);

        try {
            UsageCheckResponse response = usageService.checkKbPageUsage(userId, companyId);

            if (!response.getAllowed()) {
                log.info("KB page creation blocked for company: {}, used: {}/{}",
                        companyId, response.getCurrentUsage(), response.getLimit());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            log.debug("KB page creation allowed for company: {}, used: {}/{}",
                    companyId, response.getCurrentUsage(), response.getLimit());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking KB page limit for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}