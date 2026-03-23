package com.broadnet.billing.service;

import com.broadnet.billing.dto.response.UsageCheckResponse;
import com.broadnet.billing.dto.response.UsageStatus;

/**
 * USAGE ENFORCEMENT SERVICE
 * Real-time usage checking and blocking against limits
 *
 * CRITICAL: These operations must be atomic and sub-100ms
 */
public interface UsageEnforcementService {

    /**
     * Check and enforce answer usage
     *
     * ATOMIC OPERATION:
     * 1. Lock CompanyBilling row (PESSIMISTIC_WRITE)
     * 2. Get effective limits (plan + addons)
     * 3. Check if usage + count exceeds limit
     * 4a. If OK: increment usage, return success
     * 4b. If exceeded: set blocked flag, return blocked
     * 5. Release lock
     * 6. Log to BillingUsageLog
     * 7. Create notification if >= 80% used
     *
     * Return: UsageCheckResponse with current usage and limit
     *
     * Special Cases:
     * - Postpaid (enterprise) customers: NEVER blocked, always return success
     * - Prepaid customers: Check against effective_answers_limit
     *
     * Performance: Must complete in < 50ms
     */
    UsageCheckResponse checkAndEnforceAnswersUsage(Long userId, Long companyId, int count);

    /**
     * Check KB page usage (read-only, no increment)
     *
     * READ-ONLY:
     * 1. Get CompanyBilling (no lock)
     * 2. Get effective KB pages limit
     * 3. Check if current usage < limit
     * 4. Return status (no state change)
     *
     * Note: Actual increment happens in KbPageService
     *
     * Return: UsageCheckResponse with current KB pages and limit
     *
     * Performance: Must complete in < 20ms
     */
    UsageCheckResponse checkKbPageUsage(Long userId, Long companyId);

    /**
     * Get usage status for display
     * Shows current usage, limits, percentages
     */
    UsageStatus getUsageStatus(Long userIn, Long companyId);
}

