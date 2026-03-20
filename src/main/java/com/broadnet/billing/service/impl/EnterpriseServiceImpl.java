package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.request.CreateEnterpriseContactRequest;
import com.broadnet.billing.dto.response.*;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.EnterpriseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * ENTERPRISE SERVICE IMPLEMENTATION
 * Handles enterprise customer inquiries and usage-based billing
 * Architecture: Manage enterprise contacts, custom pricing, usage billing
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EnterpriseServiceImpl implements EnterpriseService {

    private final BillingEnterpriseContactRepository contactRepository;
    private final BillingEnterprisePricingRepository pricingRepository;
    private final BillingEnterpriseUsageBillingRepository usageBillingRepository;
    private final CompanyBillingRepository billingRepository;
    private final BillingNotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Submit enterprise contact request
     *
     * Creates:
     * - BillingEnterpriseContact with status "pending"
     * - BillingNotification for sales team
     *
     * Sends:
     * - Confirmation email to contact
     * - Alert to sales team
     */
    @Override
    @Transactional
    public EnterpriseContactResponse submitContactRequest(
            CreateEnterpriseContactRequest request,
            Long companyId) {

        log.info("Submitting enterprise contact request from: {}", request.getEmail());

        // Create contact record
        BillingEnterpriseContact contact = BillingEnterpriseContact.builder()
                .companyId(companyId)
                .contactType(BillingEnterpriseContact.ContactType.valueOf(request.getContactType()))
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .jobTitle(request.getJobTitle())
                .companyName(request.getCompanyName())
                .companySize(BillingEnterpriseContact.CompanySize.valueOf(request.getCompanySize()))
                .message(request.getMessage())
                .status(BillingEnterpriseContact.Status.pending)
                .budgetRange(request.getBudgetRange())
                .preferredContactMethod(BillingEnterpriseContact.ContactMethod.valueOf(request.getPreferredContactMethod()))
                .preferredContactTime(request.getPreferredContactTime())
                .build();

        // Set estimated usage if provided
        if (request.getEstimatedUsage() != null) {
            String estimatedUsageJson = buildEstimatedUsageJson(request.getEstimatedUsage());
            contact.setEstimatedUsage(estimatedUsageJson);
        }

        BillingEnterpriseContact savedContact = contactRepository.save(contact);

        // Create notification for sales team
        createSalesTeamNotification(savedContact);

        // TODO: Send confirmation email to contact
        // TODO: Send alert to sales team

        log.info("Enterprise contact submitted - ID: {}, Email: {}",
                savedContact.getId(), request.getEmail());

        return EnterpriseContactResponse.builder()
                .id(savedContact.getId())
                .status(savedContact.getStatus().name())
                .message("Your inquiry has been submitted. Our sales team will contact you within 24 hours.")
                .build();
    }

    /**
     * Get enterprise customer summary
     *
     * Only for postpaid (enterprise) customers
     *
     * Fetches:
     * - BillingEnterprisePricing (active)
     * - Current period from BillingEnterpriseUsageBilling
     * - Usage and calculated amount
     */
    @Override
    @Transactional(readOnly = true)
    public EnterpriseSummaryResponse getEnterpriseSummary(Long companyId) {

        log.debug("Getting enterprise summary for company: {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        // Check if enterprise customer
        if (billing.getBillingMode() != CompanyBilling.BillingMode.postpaid) {
            throw new NotEnterpriseCustomerException(companyId);
        }

        // Get active enterprise pricing
        BillingEnterprisePricing pricing = pricingRepository
                .findActivePricingByCompanyId(companyId, LocalDateTime.now())
                .orElseThrow(() -> new EnterprisePricingNotFoundException(companyId));

        // Get current period usage
        BillingEnterpriseUsageBilling currentPeriod = usageBillingRepository
                .findByCompanyAndPeriod(
                        companyId,
                        billing.getCurrentBillingPeriodStart(),
                        billing.getCurrentBillingPeriodEnd())
                .orElse(null);

        // Build current period DTO
        EnterpriseSummaryResponse.CurrentPeriodDto currentPeriodDto = null;
        if (currentPeriod != null) {
            EnterpriseSummaryResponse.CurrentPeriodDto.UsageDto usageDto =
                    EnterpriseSummaryResponse.CurrentPeriodDto.UsageDto.builder()
                            .answers(currentPeriod.getAnswersUsed() != null ? currentPeriod.getAnswersUsed().longValue() : 0L)
                            .kbPages(currentPeriod.getKbPagesUsed() != null ? currentPeriod.getKbPagesUsed().longValue() : 0L)
                            .agents(currentPeriod.getAgentsUsed())
                            .users(currentPeriod.getUsersUsed())
                            .build();

            currentPeriodDto = EnterpriseSummaryResponse.CurrentPeriodDto.builder()
                    .start(billing.getCurrentBillingPeriodStart())
                    .end(billing.getCurrentBillingPeriodEnd())
                    .usage(usageDto)
                    .calculatedAmount(currentPeriod.getSubtotalCents() != null ? currentPeriod.getSubtotalCents().longValue() : null)
                    .build();
        }

        return EnterpriseSummaryResponse.builder()
                .billingMode(billing.getBillingMode().name())
                .pricingTier(pricing.getPricingTier().name())
                .monthlyMinimum(pricing.getMinimumMonthlyCommitmentCents() != null ?
                        pricing.getMinimumMonthlyCommitmentCents().longValue() : null)
                .contractReference(pricing.getContractReference())
                .rates(EnterpriseSummaryResponse.RatesDto.builder()
                        .answersRateCents(pricing.getAnswersRateCents())
                        .kbPagesRateCents(pricing.getKbPagesRateCents())
                        .agentsRateCents(pricing.getAgentsRateCents())
                        .usersRateCents(pricing.getUsersRateCents())
                        .build())
                .currentPeriod(currentPeriodDto)
                .build();
    }

    /**
     * Get usage-based billing periods
     *
     * Shows:
     * - All BillingEnterpriseUsageBilling records
     * - Usage for each period
     * - Calculated amounts (usage * rates)
     * - Invoice status
     */
    @Override
    @Transactional(readOnly = true)
    public List<BillingPeriodResponse> getBillingPeriods(Long companyId, int limit) {

        log.debug("Getting billing periods for company: {}, limit: {}", companyId, limit);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        // Check if enterprise customer
        if (billing.getBillingMode() != CompanyBilling.BillingMode.postpaid) {
            throw new NotEnterpriseCustomerException(companyId);
        }

        // Get periods (newest first)
        Page<BillingEnterpriseUsageBilling> periods = usageBillingRepository
                .findByCompanyId(companyId, PageRequest.of(0, limit));

        return periods.stream()
                .map(this::mapToPeriodResponse)
                .collect(Collectors.toList());
    }

    // ===== Helper Methods =====

    /**
     * Build estimated usage JSON
     */
    private String buildEstimatedUsageJson(CreateEnterpriseContactRequest.EstimatedUsage usage) {
        if (usage == null) return null;

        try {
            Map<String, Object> usageMap = new HashMap<>();
            usageMap.put("answers_per_month", usage.getAnswersPerMonth() != null ? usage.getAnswersPerMonth() : 0);
            usageMap.put("kb_pages", usage.getKbPages() != null ? usage.getKbPages() : 0);
            usageMap.put("agents", usage.getAgents() != null ? usage.getAgents() : 0);
            usageMap.put("users", usage.getUsers() != null ? usage.getUsers() : 0);

            return objectMapper.writeValueAsString(usageMap);
        } catch (JsonProcessingException e) {
            log.error("Error serializing estimated usage: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Create sales team notification
     */
    private void createSalesTeamNotification(BillingEnterpriseContact contact) {
        try {
            BillingNotification notification = BillingNotification.builder()
                    .companyId(null) // Null means for sales team
                    .notificationType(BillingNotification.NotificationType.invoice_created) // Using available enum
                    .title("New Enterprise Inquiry")
                    .message(String.format(
                            "New enterprise contact from %s (%s) - %s",
                            contact.getName(),
                            contact.getCompanyName(),
                            contact.getContactType()
                    ))
                    .severity(BillingNotification.Severity.info)
                    .isRead(false)
                    .build();

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.warn("Failed to create sales team notification: {}", e.getMessage());
        }
    }

    /**
     * Map to BillingPeriodResponse
     */
    private BillingPeriodResponse mapToPeriodResponse(BillingEnterpriseUsageBilling period) {

        return BillingPeriodResponse.builder()
                .periodStart(period.getBillingPeriodStart())
                .periodEnd(period.getBillingPeriodEnd())
                .status(period.getBillingStatus().name())
                .usage(BillingPeriodResponse.UsageDto.builder()
                        .answers(period.getAnswersUsed())
                        .kbPages(period.getKbPagesUsed())
                        .agents(period.getAgentsUsed())
                        .users(period.getUsersUsed())
                        .build())
                .amounts(BillingPeriodResponse.AmountsDto.builder()
                        .answers(period.getAnswersAmountCents())
                        .kbPages(period.getKbPagesAmountCents())
                        .agents(period.getAgentsAmountCents())
                        .users(period.getUsersAmountCents())
                        .build())
                .subtotal(period.getSubtotalCents())
                .tax(period.getTaxAmountCents())
                .total(period.getTotalCents())
                .invoiceId(period.getInvoiceId())
                .stripeInvoiceId(period.getStripeInvoiceId())
                .paid(period.getBillingStatus() == BillingEnterpriseUsageBilling.BillingStatus.paid)
                .build();
    }
}