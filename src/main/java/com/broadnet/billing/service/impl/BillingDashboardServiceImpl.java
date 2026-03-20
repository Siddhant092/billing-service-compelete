package com.broadnet.billing.service.impl;

// ===== 4. BillingDashboardService =====

import com.broadnet.billing.dto.response.*;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.BillingPlanNotFoundException;
import com.broadnet.billing.exception.CompanyBillingNotFoundException;
import com.broadnet.billing.exception.UnauthorizedAccessException;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.BillingDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
class BillingDashboardServiceImpl implements BillingDashboardService {

    private final CompanyBillingRepository billingRepository;
    private final BillingNotificationRepository notificationRepository;
    private final BillingPaymentMethodRepository paymentMethodRepository;
    private final BillingAddonRepository addonRepository;
    private final BillingInvoiceRepository invoiceRepository;
    private final BillingPlanRepository planRepository;
    private final BillingPlanLimitRepository planLimitRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<BillingNotificationResponse> getNotifications(Long companyId, Pageable pageable) {
        Page<BillingNotification> notifications = notificationRepository
                .findUnreadByCompanyId(companyId, pageable);

        return notifications.map(this::mapNotificationResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentPlanResponse getCurrentPlan(Long companyId) {
        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        BillingPlan plan = planRepository.findById(billing.getActivePlan().getId())
                .orElseThrow(() -> new BillingPlanNotFoundException(billing.getActivePlanCode()));

        return CurrentPlanResponse.builder()
                .planCode(billing.getActivePlanCode())
                .planName(plan.getPlanName())
                .supportTier(String.valueOf(plan.getSupportTier()))
                .activeAddons(Collections.singletonList(billing.getActiveAddonCodes()))
                .limits(buildLimitsMap(billing))
                .billingInterval(String.valueOf(billing.getBillingInterval()))
                .periodStart(billing.getPeriodStart())
                .periodEnd(billing.getPeriodEnd())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BillingSnapshotResponse getBillingSnapshot(Long companyId) {
        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        BillingPaymentMethod paymentMethod = paymentMethodRepository
                .findDefaultByCompanyId(companyId)
                .orElse(null);

        return BillingSnapshotResponse.builder()
                .subscriptionStatus(String.valueOf(billing.getSubscriptionStatus()))
//                .paymentMethod(mapPaymentMethod(paymentMethod))
//                .serviceRestricted(billing.isServiceRestricted())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsageMetricsResponse getUsageMetrics(Long companyId) {
        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        return UsageMetricsResponse.builder()
                .answers(buildMetricBreakdown(
                        billing.getAnswersUsedInPeriod(),
                        billing.getEffectiveAnswersLimit()))
                .kbPages(buildMetricBreakdown(
                        billing.getKbPagesTotal(),
                        billing.getEffectiveKbPagesLimit()))
                .agents(buildMetricBreakdown(
                        billing.getAgentsTotal(),
                        billing.getEffectiveAgentsLimit()))
                .users(buildMetricBreakdown(
                        billing.getUsersTotal(),
                        billing.getEffectiveUsersLimit()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableBoostResponse> getAvailableBoosts(Long companyId, String category) {
        List<BillingAddon> addons = category != null
                ? addonRepository.findActiveByCategory(category)
                : addonRepository.findAllActiveAddons();

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        List<String> activeAddons = billing.getActiveAddonCodes() != null
                ? Collections.singletonList(billing.getActiveAddonCodes())
                : new ArrayList<>();

        return addons.stream()
                .map(addon -> mapBoostResponse(addon, activeAddons.contains(addon.getAddonCode())))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview(Long companyId) {
        return DashboardOverviewResponse.builder()
                .currentPlan(getCurrentPlan(companyId))
                .billingSnapshot(getBillingSnapshot(companyId))
                .usageMetrics(getUsageMetrics(companyId))
                .notifications(getNotifications(companyId,
                        org.springframework.data.domain.PageRequest.of(0, 5))
                        .getContent())
                .availableBoosts(getAvailableBoosts(companyId, null))
                .build();
    }

    @Override
    @Transactional
    public void markNotificationAsRead(Long notificationId, Long companyId) {
        BillingNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getCompanyId().equals(companyId)) {
            throw new UnauthorizedAccessException("Not authorized to access this notification");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long companyId) {
        BillingNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));

        if (!notification.getCompanyId().equals(companyId)) {
            throw new UnauthorizedAccessException("Not authorized to delete this notification");
        }

        notificationRepository.deleteById(notificationId);
    }

    @Override
    @Transactional
    public BoostPurchaseResponse purchaseBoost(Long companyId, String addonCode, String billingInterval) {
        // Implementation for purchasing boosts - similar to plan change
        // Update CompanyBilling with new addon, recalculate limits, create invoice
        return BoostPurchaseResponse.builder()
                .success(true)
                .message("Boost purchased successfully")
                .addonCode(addonCode)
                .purchaseDate(LocalDateTime.now())
                .build();
    }

    private BillingNotificationResponse mapNotificationResponse(BillingNotification n) {
        return BillingNotificationResponse.builder()
                .id(n.getId())
                .type(String.valueOf(n.getNotificationType()))
                .title(n.getTitle())
                .message(n.getMessage())
                .severity(String.valueOf(n.getSeverity()))
                .isRead(n.getIsRead())
                .createdAt(n.getCreatedAt())
                .expiresAt(n.getExpiresAt())
                .build();
    }

    private BillingDetailsResponse.PaymentMethodDetailsDto mapPaymentMethod(BillingPaymentMethod pm) {
        if (pm == null) return null;
        return BillingDetailsResponse.PaymentMethodDetailsDto.builder()
                .type(String.valueOf(pm.getType()))
                .brand(pm.getCardBrand())
                .last4(pm.getCardLast4())
                .expMonth(pm.getCardExpMonth())
                .expYear(pm.getCardExpYear())
                .isDefault(pm.getIsDefault())
                .isExpired(pm.getIsExpired())
                .build();
    }

    private UsageMetricsResponse.MetricBreakdown buildMetricBreakdown(int used, int limit) {
        int percentage = limit > 0 ? (used * 100) / limit : 0;
        return UsageMetricsResponse.MetricBreakdown.builder()
                .used(used)
                .limit(limit)
                .percentage(percentage)
                .warningLevel(percentage >= 80 ? "critical" : percentage >= 50 ? "warning" : "ok")
                .build();
    }

    private Map<String, Integer> buildLimitsMap(CompanyBilling billing) {
        return Map.of(
                "answers", billing.getEffectiveAnswersLimit(),
                "kb_pages", billing.getEffectiveKbPagesLimit(),
                "agents", billing.getEffectiveAgentsLimit(),
                "users", billing.getEffectiveUsersLimit()
        );
    }

    private AvailableBoostResponse mapBoostResponse(BillingAddon addon, boolean alreadyActive) {
        return AvailableBoostResponse.builder()
                .addonCode(addon.getAddonCode())
                .addonName(addon.getAddonName())
                .category(String.valueOf(addon.getCategory()))
                .tier(String.valueOf(addon.getTier()))
                .alreadyActive(alreadyActive)
                .build();
    }
}
