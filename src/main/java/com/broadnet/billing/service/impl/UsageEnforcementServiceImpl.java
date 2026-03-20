package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.response.UsageCheckResponse;
import com.broadnet.billing.dto.response.UsageStatus;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.UsageEnforcementService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ============================================================================
 * USAGE ENFORCEMENT SERVICE IMPLEMENTATION
 * Real-time usage checking and blocking with pessimistic locking
 * CRITICAL: This service enforces hard limits - determines if user can proceed
 * Architecture: Pessimistic locking for atomicity, prevents race conditions
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsageEnforcementServiceImpl implements UsageEnforcementService {

    // ===== Dependencies =====
    private final EntityManager entityManager;
    private final CompanyBillingRepository billingRepository;
    private final BillingPlanRepository planRepository;
    private final BillingPlanLimitRepository planLimitRepository;
    private final BillingAddonRepository addonRepository;
    private final BillingAddonDeltaRepository addonDeltaRepository;
    private final BillingUsageLogRepository usageLogRepository;
    private final BillingNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    // ===== Service Methods =====

    /**
     * Check and enforce answer usage with pessimistic locking
     *
     * ATOMIC OPERATION - Critical Section:
     * 1. Lock CompanyBilling row (PESSIMISTIC_WRITE)
     * 2. Calculate effective limit (plan + addons)
     * 3. Check if usage + count <= limit
     * 4. If OK: Increment usage, return success
     * 5. If exceeded: Set blocked flag, return blocked
     * 6. Release lock
     * 7. Log usage event
     * 8. Create notification if >= 80% used
     *
     * Performance: Must complete in < 50ms
     *
     * @param companyId Company ID
     * @param count Number of answers to increment (usually 1)
     * @return UsageCheckResponse with current usage and limit
     */
    @Override
    @Transactional
    public UsageCheckResponse checkAndEnforceAnswersUsage(Long companyId, int count) {

        long startTime = System.currentTimeMillis();
        log.debug("Checking answer usage for company {}, count: {}", companyId, count);

        try {
            // Step 1: Find and lock CompanyBilling with pessimistic lock
            CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                    .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

            // Apply pessimistic write lock
            CompanyBilling lockedBilling = entityManager.find(
                    CompanyBilling.class,
                    billing.getId(),
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (lockedBilling == null) {
                throw new CompanyBillingNotFoundException(companyId);
            }

            // Step 2: Check if enterprise (postpaid) - they never get blocked
            if (lockedBilling.getBillingMode() == CompanyBilling.BillingMode.postpaid) {
                // Enterprise customer: always allow
                lockedBilling.setAnswersUsedInPeriod(
                        lockedBilling.getAnswersUsedInPeriod() + count
                );
                entityManager.merge(lockedBilling);
                entityManager.flush();

                logUsage(companyId, BillingUsageLog.UsageType.answer, count, false, null);

                return buildSuccessResponse(
                        lockedBilling.getAnswersUsedInPeriod(),
                        Integer.MAX_VALUE,  // Enterprise has effectively unlimited answers
                        false
                );
            }

            // Step 3: Calculate effective limit (plan + addons)
            int effectiveLimit = calculateAnswersLimit(lockedBilling, LocalDateTime.now());
            int currentUsage = lockedBilling.getAnswersUsedInPeriod() != null ?
                    lockedBilling.getAnswersUsedInPeriod() : 0;
            int availableQuota = effectiveLimit - currentUsage;

            // Step 4: Check if usage allowed
            boolean allowed = count <= availableQuota;

            UsageCheckResponse response = new UsageCheckResponse();
            response.setCurrentUsage(currentUsage);
            response.setLimit(effectiveLimit);
            response.setRemaining(Math.max(0, effectiveLimit - currentUsage - count));
            response.setUsagePercentage(effectiveLimit > 0 ?
                    (double) (currentUsage + (allowed ? count : 0)) / effectiveLimit * 100 : 0);

            if (allowed) {
                // Step 5a: Usage allowed - increment counter
                lockedBilling.setAnswersUsedInPeriod(currentUsage + count);
                lockedBilling.setAnswersBlocked(false);
                response.setSuccess(true);
                response.setBlocked(false);
                response.setAllowed(true);
                response.setMessage("Answer usage incremented successfully");

                log.debug("Answer usage allowed for company {}: {}/{}",
                        companyId, lockedBilling.getAnswersUsedInPeriod(), effectiveLimit);

            } else {
                // Step 5b: Usage blocked - set flag
                lockedBilling.setAnswersBlocked(true);
                response.setSuccess(false);
                response.setBlocked(true);
                response.setAllowed(false);
                response.setError("ANSWER_LIMIT_EXCEEDED");
                response.setMessage("You have reached your monthly answer limit");

                log.warn("Answer usage BLOCKED for company {}: {}/{}",
                        companyId, currentUsage, effectiveLimit);
            }

            // Persist changes
            entityManager.merge(lockedBilling);
            entityManager.flush();

            // Step 6: Log usage event
            logUsage(companyId, BillingUsageLog.UsageType.answer, count, !allowed,
                    !allowed ? "Limit exceeded" : null);

            // Step 7: Create notification if approaching limit (80%+)
            if (allowed && response.getUsagePercentage() >= 80) {
                createWarningNotification(
                        companyId,
                        BillingNotification.NotificationType.limit_warning,
                        "Answer Usage Warning",
                        String.format("You've used %.0f%% of your monthly answers",
                                response.getUsagePercentage())
                );
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("Answer usage check completed in {}ms for company {}", duration, companyId);

            return response;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("Error checking answer usage for company {} ({}ms): {}",
                    companyId, duration, e.getMessage(), e);
            throw new InvalidBillingOperationException(
                    "Failed to check usage: " + e.getMessage(), e);
        }
    }

    /**
     * Check KB page usage (read-only, no increment)
     *
     * READ-ONLY operation - lightweight check:
     * 1. Get CompanyBilling (no lock needed)
     * 2. Get effective KB pages limit
     * 3. Check if current usage < limit
     * 4. Return status (no state change)
     *
     * Note: Actual increment happens in KbPageService when KB page is created
     *
     * Performance: Must complete in < 20ms
     *
     * @param companyId Company ID
     * @return UsageCheckResponse with KB pages usage status
     */
    @Override
    @Transactional(readOnly = true)
    public UsageCheckResponse checkKbPageUsage(Long companyId) {

        log.debug("Checking KB page usage for company {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        // Enterprise customers: always allowed
        if (billing.getBillingMode() == CompanyBilling.BillingMode.postpaid) {
            return buildSuccessResponse(
                    billing.getKbPagesTotal() != null ? billing.getKbPagesTotal() : 0,
                    Integer.MAX_VALUE,
                    false
            );
        }

        int effectiveLimit = calculateKbPagesLimit(billing, LocalDateTime.now());
        int currentUsage = billing.getKbPagesTotal() != null ? billing.getKbPagesTotal() : 0;

        boolean allowed = currentUsage < effectiveLimit;

        UsageCheckResponse response = new UsageCheckResponse();
        response.setCurrentUsage(currentUsage);
        response.setLimit(effectiveLimit);
        response.setRemaining(Math.max(0, effectiveLimit - currentUsage));
        response.setAllowed(allowed);

        if (allowed) {
            response.setSuccess(true);
            response.setBlocked(false);
            response.setMessage("KB page creation allowed");
        } else {
            response.setSuccess(false);
            response.setBlocked(true);
            response.setError("KB_PAGE_LIMIT_EXCEEDED");
            response.setMessage("You have reached your KB page limit");
        }

        return response;
    }

    /**
     * Get comprehensive usage status
     */
    @Override
    @Transactional(readOnly = true)
    public UsageStatus getUsageStatus(Long companyId) {

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        LocalDateTime now = LocalDateTime.now();

        UsageStatus status = new UsageStatus();

        // Answers
        int answersLimit = calculateAnswersLimit(billing, now);
        int answersUsed = billing.getAnswersUsedInPeriod() != null ?
                billing.getAnswersUsedInPeriod() : 0;
        status.setAnswersStatus(buildMetricStatus(answersUsed, answersLimit,
                billing.getAnswersPeriodStart()));

        // KB Pages
        int kbLimit = calculateKbPagesLimit(billing, now);
        int kbUsed = billing.getKbPagesTotal() != null ? billing.getKbPagesTotal() : 0;
        status.setKbPagesStatus(buildMetricStatus(kbUsed, kbLimit, null));

        // Agents
        int agentsLimit = billing.getEffectiveAgentsLimit() != null ?
                billing.getEffectiveAgentsLimit() : 0;
        int agentsUsed = billing.getAgentsTotal() != null ? billing.getAgentsTotal() : 0;
        status.setAgentsStatus(buildMetricStatus(agentsUsed, agentsLimit, null));

        // Users
        int usersLimit = billing.getEffectiveUsersLimit() != null ?
                billing.getEffectiveUsersLimit() : 0;
        int usersUsed = billing.getUsersTotal() != null ? billing.getUsersTotal() : 0;
        status.setUsersStatus(buildMetricStatus(usersUsed, usersLimit, null));

        return status;
    }

    // ===== Helper Methods =====

    /**
     * Calculate effective answers limit (plan + addons)
     */
    private int calculateAnswersLimit(CompanyBilling billing, LocalDateTime asOfDate) {
        int baseLimit = billing.getEffectiveAnswersLimit() != null ?
                billing.getEffectiveAnswersLimit() : 0;

        // Parse active addon codes from JSON
        List<String> addonCodes = parseAddonCodes(billing.getActiveAddonCodes());

        if (!addonCodes.isEmpty()) {
            int additionalLimit = 0;
            for (String addonCode : addonCodes) {
                BillingAddon addon = addonRepository.findByAddonCode(addonCode)
                        .orElse(null);

                if (addon != null) {
                    BillingAddonDelta delta = addonDeltaRepository
                            .findActiveByAddonAndType(addon.getId(),
                                    String.valueOf(BillingAddonDelta.DeltaType.answers_per_period),
                                    asOfDate)
                            .orElse(null);

                    if (delta != null) {
                        additionalLimit += delta.getDeltaValue();
                    }
                }
            }
            return baseLimit + additionalLimit;
        }

        return baseLimit;
    }

    /**
     * Calculate effective KB pages limit (plan + addons)
     */
    private int calculateKbPagesLimit(CompanyBilling billing, LocalDateTime asOfDate) {
        int baseLimit = billing.getEffectiveKbPagesLimit() != null ?
                billing.getEffectiveKbPagesLimit() : 0;

        // Parse active addon codes from JSON
        List<String> addonCodes = parseAddonCodes(billing.getActiveAddonCodes());

        if (!addonCodes.isEmpty()) {
            int additionalLimit = 0;
            for (String addonCode : addonCodes) {
                BillingAddon addon = addonRepository.findByAddonCode(addonCode)
                        .orElse(null);

                if (addon != null) {
                    BillingAddonDelta delta = addonDeltaRepository
                            .findActiveByAddonAndType(addon.getId(),
                                    String.valueOf(BillingAddonDelta.DeltaType.kb_pages),
                                    asOfDate)
                            .orElse(null);

                    if (delta != null) {
                        additionalLimit += delta.getDeltaValue();
                    }
                }
            }
            return baseLimit + additionalLimit;
        }

        return baseLimit;
    }

    /**
     * Parse addon codes from JSON string
     */
    private List<String> parseAddonCodes(String activeAddonCodesJson) {
        if (activeAddonCodesJson == null || activeAddonCodesJson.trim().isEmpty()) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(activeAddonCodesJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Error parsing active addon codes JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Log usage event
     */
    private void logUsage(Long companyId, BillingUsageLog.UsageType usageType, int count,
                          boolean wasBlocked, String blockReason) {
        BillingUsageLog log = BillingUsageLog.builder()
                .companyId(companyId)
                .usageType(usageType)
                .usageCount(count)
                .wasBlocked(wasBlocked)
                .blockReason(blockReason)
                .build();

        usageLogRepository.save(log);
    }

    /**
     * Create usage warning notification
     */
    private void createWarningNotification(Long companyId,
                                           BillingNotification.NotificationType type,
                                           String title,
                                           String message) {
        try {
            BillingNotification notification = BillingNotification.builder()
                    .companyId(companyId)
                    .notificationType(type)
                    .title(title)
                    .message(message)
                    .severity(BillingNotification.Severity.warning)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create notification for company {}: {}", companyId, e.getMessage());
        }
    }

    /**
     * Build success response
     */
    private UsageCheckResponse buildSuccessResponse(int used, int limit, boolean blocked) {
        UsageCheckResponse response = new UsageCheckResponse();
        response.setCurrentUsage(used);
        response.setLimit(limit);
        response.setRemaining(Math.max(0, limit - used));
        response.setSuccess(!blocked);
        response.setBlocked(blocked);
        response.setAllowed(!blocked);
        return response;
    }

    /**
     * Build metric status
     */
    private UsageStatus.MetricStatus buildMetricStatus(int used, int limit, LocalDateTime resetAt) {
        double percentage = limit > 0 ? (double) used / limit * 100 : 0;

        String warningLevel = "ok";
        if (percentage >= 80) {
            warningLevel = "critical";
        } else if (percentage >= 50) {
            warningLevel = "warning";
        }

        return UsageStatus.MetricStatus.builder()
                .used(used)
                .limit(limit)
                .percentage(percentage)
                .warningLevel(warningLevel)
                .resetAt(resetAt)
                .build();
    }
}