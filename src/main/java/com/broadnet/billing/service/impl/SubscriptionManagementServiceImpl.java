package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.response.*;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.*;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.*;
import com.stripe.param.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ============================================================================
 * SUBSCRIPTION MANAGEMENT SERVICE IMPLEMENTATION
 * Handles plan changes, cancellations, and reactivations
 * Architecture: Manage subscriptions, sync with Stripe, track entitlement changes
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionManagementServiceImpl implements SubscriptionManagementService {

    private final BillingPlanRepository planRepository;
    private final BillingPlanLimitRepository planLimitRepository;
    private final CompanyBillingRepository billingRepository;
    private final BillingEntitlementHistoryRepository entitlementHistoryRepository;
    private final BillingNotificationRepository notificationRepository;
    private final BillingStripePriceRepository priceRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    /**
     * Get all available plans
     * Architecture: Filter active plans, exclude enterprise unless requested
     */
    @Override
    @Transactional(readOnly = true)
    public List<AvailablePlanResponse> getAvailablePlans(Long userId, boolean includeEnterprise) {
        if (userId == null || userId == 0L) {
            log.error("userId is null or empty in getAvailablePlans");
        }

        List<BillingPlan> plans = planRepository.findAllActivePlans();

        return plans.stream()
                .filter(p -> includeEnterprise || !p.getIsEnterprise())
                .map(this::mapToPlanResponse)
                .collect(Collectors.toList());
    }

    /**
     * Change subscription plan
     * Architecture: Update plan, recalculate limits, create history, sync with Stripe
     */
    @Override
    @Transactional
    public SubscriptionResponse changePlan(Long userId, Long companyId, String newPlanCode, String billingInterval) {

        if (userId == null || userId == 0L) {
            log.error("userId is null or empty in changePlan");
        }
        log.info("Changing plan for company {} to {}", companyId, newPlanCode);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        String currentPlanCode = billing.getActivePlanCode();
        if (currentPlanCode != null && currentPlanCode.equals(newPlanCode)) {
            throw new SamePlanChangeException(newPlanCode);
        }

        BillingPlan newPlan = planRepository.findByPlanCode(newPlanCode)
                .orElseThrow(() -> new BillingPlanNotFoundException(newPlanCode));

        if (!newPlan.getIsActive()) {
            throw new InactivePlanException(newPlanCode);
        }

        // Store old plan for history
        String oldPlanCode = billing.getActivePlanCode();
        Integer oldAnswersLimit = billing.getEffectiveAnswersLimit();
        Integer oldKbPagesLimit = billing.getEffectiveKbPagesLimit();
        Integer oldAgentsLimit = billing.getEffectiveAgentsLimit();
        Integer oldUsersLimit = billing.getEffectiveUsersLimit();

        // Update plan
        billing.setActivePlanCode(newPlanCode);
        billing.setActivePlan(newPlan);

        // Recalculate limits
        recalculatePlanLimits(billing, newPlan);

        billingRepository.save(billing);

        // Create history entry
        BillingEntitlementHistory history = BillingEntitlementHistory.builder()
                .companyId(companyId)
                .changeType(BillingEntitlementHistory.ChangeType.plan_change)
                .oldPlanCode(oldPlanCode)
                .newPlanCode(newPlanCode)
                .oldAnswersLimit(oldAnswersLimit)
                .newAnswersLimit(billing.getEffectiveAnswersLimit())
                .oldKbPagesLimit(oldKbPagesLimit)
                .newKbPagesLimit(billing.getEffectiveKbPagesLimit())
                .oldAgentsLimit(oldAgentsLimit)
                .newAgentsLimit(billing.getEffectiveAgentsLimit())
                .oldUsersLimit(oldUsersLimit)
                .newUsersLimit(billing.getEffectiveUsersLimit())
                .triggeredBy(BillingEntitlementHistory.TriggeredBy.api)
                .effectiveDate(LocalDateTime.now())
                .build();

        entitlementHistoryRepository.save(history);

        // Create notification
        createNotification(
                companyId,
                BillingNotification.NotificationType.plan_changed,
                "Plan Updated",
                String.format("Your plan has been changed to %s", newPlan.getPlanName())
        );

        // Sync with Stripe (if has subscription)
        if (billing.getStripeSubscriptionId() != null) {
            syncWithStripe(billing, newPlan, billingInterval);
        }

        return SubscriptionResponse.builder()
                .success(true)
                .message("Plan changed successfully")
                .activePlanCode(newPlanCode)
                .previousPlanCode(oldPlanCode)
                .effectiveDate(LocalDateTime.now())
                .status(billing.getSubscriptionStatus() != null ?
                        billing.getSubscriptionStatus().name() : "active")
                .build();
    }

    /**
     * Cancel subscription
     * Architecture: Update status, set cancellation dates, create history
     */
    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(Long userId, Long companyId, boolean cancelAtPeriodEnd, String reason) {

        if (userId == null || userId == 0L) {
            log.error("userId is null or empty in cancelSubscription");
        }
        log.info("Canceling subscription for company {}, atPeriodEnd: {}", companyId, cancelAtPeriodEnd);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        LocalDateTime now = LocalDateTime.now();

        billing.setSubscriptionStatus(CompanyBilling.SubscriptionStatus.canceled);
        if (cancelAtPeriodEnd) {
            billing.setCancelAtPeriodEnd(true);
            billing.setCancelAt(billing.getPeriodEnd());
        } else {
            billing.setCanceledAt(now);
        }

        billingRepository.save(billing);

        // Create history
        BillingEntitlementHistory history = BillingEntitlementHistory.builder()
                .companyId(companyId)
                .changeType(BillingEntitlementHistory.ChangeType.limit_update) // Using available enum
                .oldPlanCode(billing.getActivePlanCode())
                .newPlanCode(billing.getActivePlanCode())
                .triggeredBy(BillingEntitlementHistory.TriggeredBy.api)
                .effectiveDate(now)
                .build();

        entitlementHistoryRepository.save(history);

        // Notification
        createNotification(
                companyId,
                BillingNotification.NotificationType.subscription_canceled,
                "Subscription Canceled",
                "Your subscription has been canceled"
        );

        // Sync with Stripe
        if (billing.getStripeSubscriptionId() != null) {
            syncCancellationWithStripe(billing, cancelAtPeriodEnd);
        }

        return SubscriptionResponse.builder()
                .success(true)
                .message("Subscription canceled")
                .status(CompanyBilling.SubscriptionStatus.canceled.name())
                .cancellationType(cancelAtPeriodEnd ? "end_of_period" : "immediate")
                .periodEndDate(billing.getPeriodEnd())
                .build();
    }

    /**
     * Reactivate subscription
     * Architecture: Check grace period, restore status, create history
     */
    @Override
    @Transactional
    public SubscriptionResponse reactivateSubscription(Long userId, Long companyId) {

        if (userId == null || userId == 0L) {
            log.error("userId is null or empty in reactivateSubscription");
        }
        log.info("Reactivating subscription for company {}", companyId);

        CompanyBilling billing = billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        if (billing.getSubscriptionStatus() != CompanyBilling.SubscriptionStatus.canceled) {
            throw new SubscriptionReactivationException(
                    "Subscription is not in canceled state");
        }

        // Check grace period (default 7 days)
        LocalDateTime canceledAt = billing.getCanceledAt();
        if (canceledAt != null) {
            long daysSinceCancellation = java.time.temporal.ChronoUnit.DAYS
                    .between(canceledAt, LocalDateTime.now());
            if (daysSinceCancellation > 7) {
                throw new GracePeriodExpiredException(companyId);
            }
        }

        billing.setSubscriptionStatus(CompanyBilling.SubscriptionStatus.active);
        billing.setCanceledAt(null);
        billing.setCancelAtPeriodEnd(null);
        billing.setCancelAt(null);

        billingRepository.save(billing);

        // History and notification
        BillingEntitlementHistory history = BillingEntitlementHistory.builder()
                .companyId(companyId)
                .changeType(BillingEntitlementHistory.ChangeType.limit_update) // Using available enum
                .oldPlanCode(billing.getActivePlanCode())
                .newPlanCode(billing.getActivePlanCode())
                .triggeredBy(BillingEntitlementHistory.TriggeredBy.api)
                .effectiveDate(LocalDateTime.now())
                .build();

        entitlementHistoryRepository.save(history);

        createNotification(
                companyId,
                BillingNotification.NotificationType.subscription_renewed,
                "Subscription Reactivated",
                "Your subscription has been reactivated"
        );

        return SubscriptionResponse.builder()
                .success(true)
                .message("Subscription reactivated")
                .status(CompanyBilling.SubscriptionStatus.active.name())
                .build();
    }

    // ===== Helper Methods =====

    /**
     * Recalculate plan limits based on new plan
     */
    private void recalculatePlanLimits(CompanyBilling billing, BillingPlan plan) {
        LocalDateTime now = LocalDateTime.now();
        List<BillingPlanLimit> limits = planLimitRepository.findLimitsEffectiveAt(plan.getId(), now);

        limits.forEach(limit -> {
            switch (limit.getLimitType()) {
                case answers_per_period:
                    billing.setEffectiveAnswersLimit(limit.getLimitValue());
                    break;
                case kb_pages:
                    billing.setEffectiveKbPagesLimit(limit.getLimitValue());
                    break;
                case agents:
                    billing.setEffectiveAgentsLimit(limit.getLimitValue());
                    break;
                case users:
                    billing.setEffectiveUsersLimit(limit.getLimitValue());
                    break;
            }
        });
    }

    /**
     * Sync plan change with Stripe
     */
    private void syncWithStripe(CompanyBilling billing, BillingPlan plan, String billingInterval) {
        try {
            Stripe.apiKey = stripeApiKey;

            BillingStripePrice price = priceRepository
                    .findActivePriceByPlanAndInterval(plan.getId(), BillingStripePrice.BillingInterval.valueOf(billingInterval))
                    .orElseThrow(() -> new StripeObjectNotFoundException("Price not found"));

            Subscription subscription = Subscription.retrieve(billing.getStripeSubscriptionId());

            subscription.update(SubscriptionUpdateParams.builder()
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscription.getItems().getData().getFirst().getId())
                            .setPrice(price.getStripePriceId())
                            .build())
                    .build());

        } catch (StripeException e) {
            log.error("Failed to sync with Stripe: {}", e.getMessage());
            throw new StripeApiException("Failed to sync plan change with Stripe", e.getCode());
        }
    }

    /**
     * Sync cancellation with Stripe
     */
    private void syncCancellationWithStripe(CompanyBilling billing, boolean atPeriodEnd) {
        try {
            Stripe.apiKey = stripeApiKey;
            Subscription subscription = Subscription.retrieve(billing.getStripeSubscriptionId());

            if (atPeriodEnd) {
                subscription.update(SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .build());
            } else {
                subscription.cancel();
            }
        } catch (StripeException e) {
            log.error("Failed to sync cancellation with Stripe: {}", e.getMessage());
            // Don't throw - cancellation already saved locally
        }
    }

    /**
     * Create notification for subscription events
     */
    private void createNotification(
            Long companyId,
            BillingNotification.NotificationType type,
            String title,
            String message) {

        BillingNotification notification = BillingNotification.builder()
                .companyId(companyId)
                .notificationType(type)
                .title(title)
                .message(message)
                .severity(BillingNotification.Severity.info)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Map BillingPlan to AvailablePlanResponse
     */
    private AvailablePlanResponse mapToPlanResponse(BillingPlan plan) {
        return AvailablePlanResponse.builder()
                .id(plan.getId())
                .planCode(plan.getPlanCode())
                .planName(plan.getPlanName())
                .description(plan.getDescription())
                .isActive(plan.getIsActive())
                .isEnterprise(plan.getIsEnterprise())
                .supportTier(plan.getSupportTier() != null ? plan.getSupportTier().name() : null)
                .build();
    }
}