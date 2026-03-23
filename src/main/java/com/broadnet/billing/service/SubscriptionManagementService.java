package com.broadnet.billing.service;


import com.broadnet.billing.dto.response.AvailablePlanResponse;
import com.broadnet.billing.dto.response.SubscriptionResponse;

import java.util.List;

/**
 * SUBSCRIPTION MANAGEMENT SERVICE
 * Handles plan changes, cancellations, reactivations
 */
public interface SubscriptionManagementService {

    /**
     * Get all available plans for subscription
     *
     * Filters:
     * - is_active = true
     * - is_enterprise = false (unless includeEnterprise=true)
     * - Include BillingPlanLimit for each plan
     * - Include BillingStripePrice for each plan
     *
     * Sort: By plan display order
     *
     * Return: List of AvailablePlanResponse with all plan details
     */
    List<AvailablePlanResponse> getAvailablePlans(Long userId, boolean includeEnterprise);

    /**
     * Change company's active plan
     *
     * Validates:
     * - New plan exists and is_active = true
     * - New plan different from current
     * - Company has active subscription
     *
     * Operations (transactional):
     * 1. Get CompanyBilling (current state)
     * 2. Store old plan for history
     * 3. Update activePlanCode with optimistic lock
     * 4. Recalculate effective limits (plan + current addons)
     * 5. Update effective limit fields
     * 6. Create BillingEntitlementHistory entry
     * 7. Create BillingNotification
     * 8. Sync with Stripe (update subscription)
     *
     * Return: SubscriptionResponse with new plan details
     *
     * Error: OptimisticLockException if version conflict (caller should retry)
     */
    SubscriptionResponse changePlan(Long userId, Long companyId, String newPlanCode, String billingInterval);

    /**
     * Cancel company's subscription
     *
     * Parameters:
     * - cancelAtPeriodEnd: true = end of billing period, false = immediate
     * - reason: optional cancellation reason for feedback
     *
     * Operations:
     * 1. Get CompanyBilling
     * 2. Update subscriptionStatus to "canceled"
     * 3. If cancelAtPeriodEnd: Set cancelAtPeriodEnd flag
     * 4. If immediate: Set canceledAt timestamp, block access
     * 5. Create BillingEntitlementHistory
     * 6. Create BillingNotification (cancellation confirmation)
     * 7. Sync with Stripe
     *
     * Return: SubscriptionResponse confirming cancellation
     */
    SubscriptionResponse cancelSubscription(Long userId, Long companyId, boolean cancelAtPeriodEnd, String reason);

    /**
     * Reactivate canceled subscription
     * Only available within grace period (default: 7 days)
     *
     * Validates:
     * - Subscription is canceled
     * - Within grace period
     *
     * Operations:
     * 1. Get canceled CompanyBilling
     * 2. Check cancellation date vs grace period
     * 3. Update subscriptionStatus to "active"
     * 4. Clear cancelationAt and related flags
     * 5. Restore access immediately
     * 6. Create BillingEntitlementHistory
     * 7. Create BillingNotification (reactivation confirmation)
     * 8. Sync with Stripe
     *
     * Return: SubscriptionResponse with active subscription
     */
    SubscriptionResponse reactivateSubscription(Long userId, Long companyId);
}
