package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.request.*;
import com.broadnet.billing.dto.response.*;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.AdminBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ADMIN BILLING SERVICE IMPLEMENTATION
 * Manages plan limits, enterprise pricing, and contact requests
 * Architecture: Admin-only operations for billing management
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminBillingServiceImpl implements AdminBillingService {

    private final BillingPlanRepository planRepository;
    private final BillingPlanLimitRepository planLimitRepository;
    private final CompanyBillingRepository billingRepository;
    private final BillingEnterprisePricingRepository pricingRepository;
    private final BillingEnterpriseContactRepository contactRepository;
    private final BillingEntitlementHistoryRepository entitlementHistoryRepository;
    private final BillingNotificationRepository notificationRepository;

    /**
     * Update plan limits (affects all companies on that plan)
     *
     * Creates:
     * - New BillingPlanLimit with effective_from date
     * - Old limit gets effective_to = new limit's effective_from
     *
     * Triggers:
     * - Background job to recalculate entitlements
     */
    @Override
    @Transactional
    public PlanLimitUpdateResponse updatePlanLimits(
            String planCode,
            UpdatePlanLimitRequest request) {

        log.info("Admin updating plan limits for plan: {}, limitType: {}",
                planCode, request.getLimitType());

        // Get plan
        BillingPlan plan = planRepository.findByPlanCode(planCode)
                .orElseThrow(() -> new BillingPlanNotFoundException(planCode));

        if (!plan.getIsActive()) {
            throw new InactivePlanException(planCode);
        }

        // Validate limit value
        if (request.getLimitValue() == null || request.getLimitValue() < 0) {
            throw new InvalidBillingOperationException(
                    "Limit value must be non-negative");
        }

        // Validate billing interval
        if (!request.getBillingInterval().matches("month|year")) {
            throw new InvalidBillingOperationException(
                    "Invalid billing interval: " + request.getBillingInterval());
        }

        // Check for duplicate (same type/interval/effective_from)
        List<BillingPlanLimit> existing = planLimitRepository
                .findLimitsEffectiveAt(plan.getId(), request.getEffectiveFrom());

        boolean hasDuplicate = existing.stream()
                .anyMatch(l -> l.getLimitType().equals(request.getLimitType()) &&
                        l.getBillingInterval().equals(request.getBillingInterval()) &&
                        l.getEffectiveFrom().equals(request.getEffectiveFrom()));

        if (hasDuplicate) {
            throw new ConflictingOperationException(
                    "Limit already exists for this type/interval/effective_from");
        }

        // Deactivate old limits
        List<BillingPlanLimit> oldLimits = planLimitRepository
                .findActiveLimitsByPlanId(plan.getId());

        // Step 1: Deactivate old limits FIRST (bulk update preferred)
        planLimitRepository.deactivateExistingLimits(
                plan.getId(),
                BillingPlanLimit.LimitType.valueOf(request.getLimitType()),
                BillingPlanLimit.BillingInterval.valueOf(request.getBillingInterval()),
                request.getEffectiveFrom()
        );

// Step 2: Create new limit
        BillingPlanLimit newLimit = new BillingPlanLimit();
        newLimit.setPlan(plan);
        newLimit.setLimitType(BillingPlanLimit.LimitType.valueOf(request.getLimitType()));
        newLimit.setLimitValue(request.getLimitValue());
        newLimit.setBillingInterval(BillingPlanLimit.BillingInterval.valueOf(request.getBillingInterval()));
        newLimit.setEffectiveFrom(request.getEffectiveFrom());
        newLimit.setIsActive(true);

        planLimitRepository.save(newLimit);

        // Find affected companies
        List<CompanyBilling> affectedCompanies = billingRepository.findAll().stream()
                .filter(cb -> cb.getActivePlan() != null) // ✅ prevent NPE
                .filter(cb -> plan.getId().equals(cb.getActivePlan().getId()))
                .collect(Collectors.toList());

        log.info("Plan limit updated - Affected companies: {}", affectedCompanies.size());

        // TODO: Trigger background job for entitlement recalculation
        String jobId = generateJobId();

        return PlanLimitUpdateResponse.builder()
                .success(true)
                .message("Plan limit updated. Recomputing entitlements for active subscriptions...")
                .affectedCompanies(affectedCompanies.size())
                .backgroundJobId(jobId)
                .build();
    }

    /**
     * Set custom enterprise pricing for a company
     *
     * Creates:
     * - BillingEnterprisePricing with custom rates
     *
     * Updates:
     * - CompanyBilling.enterprisePricingId
     * - CompanyBilling.billingMode = "postpaid"
     */
    @Override
    @Transactional
    public EnterprisePricingResponse setEnterprisePricing(
            SetEnterprisePricingRequest request) {

        log.info("Admin setting enterprise pricing for company: {}", request.getCompanyId());

        // Verify company exists
        CompanyBilling billing = billingRepository.findByCompanyId(request.getCompanyId())
                .orElseThrow(() -> new CompanyBillingNotFoundException(request.getCompanyId()));

        // Validate pricing values
        if (request.getAnswersRateCents() == null || request.getAnswersRateCents() < 0) {
            throw new InvalidBillingOperationException("Answers rate must be non-negative");
        }
        if (request.getKbPagesRateCents() == null || request.getKbPagesRateCents() < 0) {
            throw new InvalidBillingOperationException("KB pages rate must be non-negative");
        }
        if (request.getAgentsRateCents() == null || request.getAgentsRateCents() < 0) {
            throw new InvalidBillingOperationException("Agents rate must be non-negative");
        }
        if (request.getUsersRateCents() == null || request.getUsersRateCents() < 0) {
            throw new InvalidBillingOperationException("Users rate must be non-negative");
        }

        // Create enterprise pricing
        BillingEnterprisePricing pricing = new BillingEnterprisePricing();
        pricing.setCompanyId(request.getCompanyId());
        pricing.setPricingTier(BillingEnterprisePricing.PricingTier.valueOf(request.getPricingTier()));
        pricing.setAnswersRateCents(request.getAnswersRateCents());
        pricing.setKbPagesRateCents(request.getKbPagesRateCents());
        pricing.setAgentsRateCents(request.getAgentsRateCents());
        pricing.setUsersRateCents(request.getUsersRateCents());
        pricing.setMinimumMonthlyCommitmentCents(request.getMinimumMonthlyCommitmentCents());
        pricing.setMinimumAnswersCommitment(Math.toIntExact(request.getMinimumAnswersCommitment()));
        pricing.setEffectiveFrom(request.getEffectiveFrom());
        pricing.setContractReference(request.getContractReference());
        pricing.setNotes(request.getNotes());
        pricing.setIsActive(true);

        BillingEnterprisePricing savedPricing = pricingRepository.save(pricing);

        // Update company billing
        billing.setEnterprisePricingId(savedPricing.getId());
        billing.setBillingMode(CompanyBilling.BillingMode.postpaid);
        // Set effective limits to very high for enterprise
        billing.setEffectiveAnswersLimit(Integer.MAX_VALUE);
        billing.setEffectiveKbPagesLimit(Integer.MAX_VALUE);
        billing.setEffectiveAgentsLimit(Integer.MAX_VALUE);
        billing.setEffectiveUsersLimit(Integer.MAX_VALUE);

        billingRepository.save(billing);

        // Create notification
//        createNotification(request.getCompanyId(),
//                "enterprise_pricing_set",
//                "Enterprise Pricing Configured",
//                "Your custom enterprise pricing has been configured");
//
//         Create history entry
//        BillingEntitlementHistory history = new BillingEntitlementHistory();
//        history.setCompanyId(request.getCompanyId());
//        history.setChangeType(/*"enterprise_pricing_set"*/ BillingEntitlementHistory.ChangeType.valueOf("enterprise_pricing_set"));
//        history.setNewValue(request.getPricingTier());
//        history.setTriggeredBy(BillingEntitlementHistory.TriggeredBy.admin);
//        entitlementHistoryRepository.save(history);

        log.info("Enterprise pricing set for company: {} - ID: {}",
                request.getCompanyId(), savedPricing.getId());

        return EnterprisePricingResponse.builder()
                .id(savedPricing.getId())
                .success(true)
                .message("Enterprise pricing set successfully")
                .pricingId(savedPricing.getId())
                .build();
    }

    /**
     * Get enterprise contact requests (CRM view)
     *
     * Filters:
     * - Optional: status filter
     * - Optional: assigned_to filter
     */
    @Override
    @Transactional(readOnly = true)
    public Page<EnterpriseContactAdminResponse> getEnterpriseContacts(
            String status,
            Long assignedTo,
            Pageable pageable) {

        log.debug("Fetching enterprise contacts - status: {}, assignedTo: {}",
                status, assignedTo);

        Page<BillingEnterpriseContact> contacts;

        if (status != null && !status.isEmpty()) {
            contacts = contactRepository.findByStatus(status, pageable);
        } else if (assignedTo != null) {
            List<BillingEnterpriseContact> assignedContacts =
                    contactRepository.findAssignedTo(assignedTo);
            contacts = new PageImpl<>(assignedContacts, pageable, assignedContacts.size());
        } else {
            // Get all pending contacts
            List<BillingEnterpriseContact> allContacts =
                    contactRepository.findPendingContacts();
            contacts = new PageImpl<>(allContacts, pageable, allContacts.size());
        }

        return contacts.map(this::mapToAdminResponse);
    }

    /**
     * Update enterprise contact status and assignment
     */
    @Override
    @Transactional
    public EnterpriseContactAdminResponse updateEnterpriseContact(
            Long contactId,
            UpdateEnterpriseContactRequest request) {

        log.info("Updating enterprise contact: {}", contactId);

        BillingEnterpriseContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact not found: " + contactId));

        // Update status if provided
        if (request.getStatus() != null) {
            validateStatusTransition(String.valueOf(contact.getStatus()), request.getStatus());
            contact.setStatus(BillingEnterpriseContact.Status.valueOf(request.getStatus()));
        }

        // Update assignment if provided
        if (request.getAssignedTo() != null) {
            contact.setAssignedTo(request.getAssignedTo());
            contact.setAssignedAt(LocalDateTime.now());
        }

        // Update outcome if provided
        if (request.getOutcome() != null) {
            contact.setOutcome(BillingEnterpriseContact.Outcome.valueOf(request.getOutcome()));
            contact.setClosedAt(LocalDateTime.now());
        }

        // Update notes if provided
        if (request.getNotes() != null) {
            contact.setNotes(request.getNotes());
        }

        BillingEnterpriseContact updated = contactRepository.save(contact);

        log.info("Enterprise contact updated: {}", contactId);

        return mapToAdminResponse(updated);
    }

    // ===== Helper Methods =====

    /**
     * Validate status transition
     */
    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Can transition from any status to any status
        // (flexibility for admin operations)
        if (newStatus == null || newStatus.isEmpty()) {
            throw new InvalidBillingOperationException("Status cannot be empty");
        }
    }

    /**
     * Generate job ID for background task
     */
    private String generateJobId() {
        return "job_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Create notification
     */
    private void createNotification(Long companyId, String type,
                                    String title, String message) {
        try {
            BillingNotification notification = new BillingNotification();
            notification.setCompanyId(companyId);
            notification.setNotificationType(BillingNotification.NotificationType.valueOf(type));
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setSeverity(BillingNotification.Severity.valueOf("info"));
            notification.setIsRead(false);

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create notification: {}", e.getMessage());
        }
    }

    /**
     * Map to admin response
     */
    private EnterpriseContactAdminResponse mapToAdminResponse(
            BillingEnterpriseContact contact) {

        EnterpriseContactAdminResponse.EstimatedUsageDto usageDto = null;
        if (contact.getEstimatedUsage() != null) {
            // Parse JSON - simplified for now
            usageDto = EnterpriseContactAdminResponse.EstimatedUsageDto.builder()
                    .build();
        }

        return EnterpriseContactAdminResponse.builder()
                .id(contact.getId())
                .companyId(contact.getCompanyId())
                .contactType(String.valueOf(contact.getContactType()))
                .name(contact.getName())
                .email(contact.getEmail())
                .phone(contact.getPhone())
                .jobTitle(contact.getJobTitle())
                .companyName(contact.getCompanyName())
                .companySize(String.valueOf(contact.getCompanySize()))
                .status(String.valueOf(contact.getStatus()))
                .assignedTo(contact.getAssignedTo())
                .assignedAt(contact.getAssignedAt())
                .firstContactedAt(contact.getFirstContactedAt())
                .message(contact.getMessage())
                .estimatedUsage(usageDto)
                .budgetRange(contact.getBudgetRange())
                .outcome(String.valueOf(contact.getOutcome()))
                .notes(contact.getNotes())
                .createdAt(contact.getCreatedAt())
                .build();
    }
}