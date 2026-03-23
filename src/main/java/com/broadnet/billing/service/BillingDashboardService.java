package com.broadnet.billing.service;


import com.broadnet.billing.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * BILLING DASHBOARD SERVICE
 * Provides data for dashboard UI
 *
 * Caching: All methods cached per TTL in controller documentation
 */
public interface BillingDashboardService {

    /**
     * Get notifications for user
     *
     * Filters:
     * - is_read = false (unread only)
     * - expires_at IS NULL OR expires_at > NOW (not expired)
     * - Sort by created_at DESC (newest first)
     *
     * Return: Paginated notifications
     */
    Page<BillingNotificationResponse> getNotifications(Long userId, Long companyId, Pageable pageable);

    /**
     * Get current active plan with all limits
     *
     * Includes:
     * - BillingPlan details
     * - Base BillingPlanLimit
     * - Applied BillingAddonDelta (cumulative)
     * - Effective limits (sum of plan + addons)
     * - Period dates (start/end of current billing cycle)
     *
     * Return: CurrentPlanResponse with complete plan information
     */
    CurrentPlanResponse getCurrentPlan(Long userId, Long companyId);

    /**
     * Get billing snapshot (payment, status, invoices)
     *
     * Includes:
     * - subscription_status from CompanyBilling
     * - default BillingPaymentMethod (non-expired)
     * - next invoice from Stripe API (or BillingInvoice)
     * - payment_failure_date if exists
     * - service_restricted flag
     *
     * Return: BillingSnapshotResponse
     */
    BillingSnapshotResponse getBillingSnapshot(Long userId, Long companyId);

    /**
     * Get usage metrics for progress bars
     *
     * For each metric type:
     * - Current usage (from BillingUsageLog aggregation)
     * - Effective limit (plan + addons)
     * - Percentage (usage / limit * 100)
     * - Warning level (< 50%: ok, 50-80%: warning, > 80%: critical)
     * - Reset date for periodic metrics
     *
     * Metrics:
     * - answers: daily usage in current period
     * - kb_pages: total pages created
     * - agents: total agents created
     * - users: total users invited
     *
     * Return: UsageMetricsResponse
     */
    UsageMetricsResponse getUsageMetrics(Long userId, Long companyId);

    /**
     * Get available boosts for purchase
     *
     * Filters:
     * - is_active = true
     * - Optional: category filter (answers, kb)
     * - Exclude already-purchased addons for this company
     * - Include pricing (BillingStripePrice)
     *
     * Return: List of AvailableBoostResponse
     */
    List<AvailableBoostResponse> getAvailableBoosts(Long userId, Long companyId, String category);

    /**
     * Get complete dashboard overview
     * Aggregates all above endpoints
     *
     * Implementation: Parallel load all data (1s timeout)
     * If some calls fail, return partial data with error flags
     *
     * Return: DashboardOverviewResponse with all dashboard data
     */
    DashboardOverviewResponse getDashboardOverview(Long userId, Long companyId);

    /**
     * Mark notification as read
     *
     * Validates:
     * - Notification exists
     * - Belongs to this company
     *
     * Updates:
     * - is_read = true
     * - read_at = NOW
     */
    void markNotificationAsRead(Long userId, Long notificationId, Long companyId);

    /**
     * Delete notification permanently
     *
     * Validates:
     * - Notification exists
     * - Belongs to this company
     */
    void deleteNotification(Long userId, Long notificationId, Long companyId);

    /**
     * Purchase a boost add-on
     *
     * Validates:
     * - Addon exists and is_active = true
     * - Not already purchased
     * - Billing interval is valid
     *
     * Operations:
     * 1. Create Stripe invoice line item for addon
     * 2. Create BillingAddonDelta (or update existing)
     * 3. Recalculate effective limits
     * 4. Update CompanyBilling with new limits
     * 5. Create BillingNotification (boost purchased)
     * 6. Create invoice
     *
     * Return: BoostPurchaseResponse with confirmation
     */
    BoostPurchaseResponse purchaseBoost(Long userId, Long companyId, String addonCode, String billingInterval);
}
