package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.PurchaseBoostRequest;
import com.broadnet.billing.dto.response.*;
import com.broadnet.billing.exception.IllegalAccessException;
import com.broadnet.billing.service.BillingDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Billing Dashboard API Controller
 * Provides data for dashboard UI display
 *
 * Security: Requires authenticated user
 * Rate Limiting: Generous (30 requests/minute)
 * Caching: Aggressive (all endpoints cached, TTL specified below)
 */
@Slf4j
@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingDashboardController {

    private final BillingDashboardService dashboardService;

    /**
     * Get unread notifications for current user
     *
     * GET /api/billing/notifications
     *
     * @param pageable Pagination (default: 10 per page, sorted by created_at DESC)
     * @return Page of BillingNotificationResponse
     *
     * Response:
     * {
     *   "content": [
     *     {
     *       "id": 1,
     *       "type": "limit_warning",
     *       "title": "Nearing Answer Limit",
     *       "message": "You've used 80% of your monthly answers",
     *       "severity": "warning",
     *       "is_read": false,
     *       "action_url": "/billing/upgrade",
     *       "created_at": "2025-03-19T10:00:00Z"
     *     },
     *     ...
     *   ],
     *   "total_elements": 5,
     *   "total_pages": 1
     * }
     *
     * Filter:
     * - Unread only (is_read = false)
     * - Not expired (expires_at > NOW)
     * - Sorted by created_at DESC
     * - Paginated (10 items default)
     *
     * Caching: 30s TTL (notifications change frequently)
     * Cache key: "notifications:{companyId}"
     */
    @GetMapping("/notifications")
    public ResponseEntity<Page<BillingNotificationResponse>> getNotifications(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId,
            Pageable pageable) {

        log.debug("Fetching notifications for company: {}, page: {}", companyId, pageable.getPageNumber());

        try {
            Page<BillingNotificationResponse> notifications = dashboardService.getNotifications(
                    userId,
                    companyId,
                    pageable
            );

            return ResponseEntity.ok(notifications);

        } catch (Exception e) {
            log.error("Error fetching notifications for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get current plan details
     *
     * GET /api/billing/current-plan
     *
     * @return CurrentPlanResponse with active plan and addons
     *
     * Response:
     * {
     *   "plan_code": "professional",
     *   "plan_name": "Professional Plan",
     *   "support_tier": "standard",
     *   "active_addons": ["answers_boost_m", "kb_boost_s"],
     *   "limits": {
     *     "answers_per_period": 5000,
     *     "kb_pages": 500,
     *     "agents": 5,
     *     "users": 10
     *   },
     *   "billing_interval": "month",
     *   "period_start": "2025-03-19T00:00:00Z",
     *   "period_end": "2025-04-19T00:00:00Z"
     * }
     *
     * Filter:
     * - Active plan only
     * - Include addon deltas in limits
     * - Current effective dates
     *
     * Caching: 5min TTL (changes infrequently)
     * Cache key: "current_plan:{companyId}"
     */
    @GetMapping("/current-plan")
    public ResponseEntity<CurrentPlanResponse> getCurrentPlan(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching current plan for company: {}", companyId);

        try {
            CurrentPlanResponse plan = dashboardService.getCurrentPlan(userId, companyId);
            return ResponseEntity.ok(plan);

        } catch (IllegalStateException e) {
            log.warn("No plan found for company: {}", companyId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching current plan for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get billing snapshot (payment method, next invoice, status)
     *
     * GET /api/billing/billing-snapshot
     *
     * @return BillingSnapshotResponse
     *
     * Response:
     * {
     *   "subscription_status": "active",
     *   "payment_method": {
     *     "type": "card",
     *     "brand": "visa",
     *     "last4": "4242",
     *     "exp_month": 12,
     *     "exp_year": 2026
     *   },
     *   "next_invoice": {
     *     "date": "2025-04-19T00:00:00Z",
     *     "amount": 9900,
     *     "currency": "usd"
     *   },
     *   "payment_failure_date": null,
     *   "service_restricted": false
     * }
     *
     * Filter:
     * - Default payment method only
     * - Non-expired payment methods
     * - Active subscription info
     *
     * Caching: 1min TTL
     * Cache key: "billing_snapshot:{companyId}"
     */
    @GetMapping("/billing-snapshot")
    public ResponseEntity<BillingSnapshotResponse> getBillingSnapshot(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching billing snapshot for company: {}", companyId);

        try {
            BillingSnapshotResponse snapshot = dashboardService.getBillingSnapshot(userId, companyId);
            return ResponseEntity.ok(snapshot);

        } catch (Exception e) {
            log.error("Error fetching billing snapshot for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get usage metrics for progress bars
     *
     * GET /api/billing/usage-metrics
     *
     * @return UsageMetricsResponse with percentages
     *
     * Response:
     * {
     *   "answers": {
     *     "used": 1500,
     *     "limit": 5000,
     *     "percentage": 30,
     *     "reset_at": "2025-04-19T00:00:00Z"
     *   },
     *   "kb_pages": {
     *     "used": 950,
     *     "limit": 1000,
     *     "percentage": 95,
     *     "warning_level": "warning"
     *   },
     *   "agents": {
     *     "used": 3,
     *     "limit": 5,
     *     "percentage": 60
     *   },
     *   "users": {
     *     "used": 8,
     *     "limit": 10,
     *     "percentage": 80
     *   }
     * }
     *
     * Filter:
     * - Current period only
     * - Include addon limits
     * - Calculate percentages
     * - Determine warning levels
     *
     * Caching: 1min TTL
     * Cache key: "usage_metrics:{companyId}"
     */
    @GetMapping("/usage-metrics")
    public ResponseEntity<UsageMetricsResponse> getUsageMetrics(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching usage metrics for company: {}", companyId);

        try {
            UsageMetricsResponse metrics = dashboardService.getUsageMetrics(userId, companyId);
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Error fetching usage metrics for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get available boost add-ons for purchase
     *
     * GET /api/billing/available-boosts
     *
     * @param category Filter by category (answers, kb) - optional
     * @return List of available boosts
     *
     * Response:
     * [
     *   {
     *     "addon_code": "answers_boost_s",
     *     "addon_name": "Small Answers Boost",
     *     "category": "answers",
     *     "tier": "small",
     *     "delta_value": 500,
     *     "delta_type": "answers_per_period",
     *     "price_monthly": 1000,
     *     "price_annual": 10000,
     *     "already_active": false
     *   },
     *   ...
     * ]
     *
     * Filter:
     * - Active addons only (is_active = true)
     * - Optional: By category (answers, kb)
     * - Exclude already purchased addons
     * - Include current pricing
     *
     * Caching: 15min TTL
     * Cache key: "available_boosts:{category?}"
     */
    @GetMapping("/available-boosts")
    public ResponseEntity<List<AvailableBoostResponse>> getAvailableBoosts(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "company_id") Long companyId,
            @RequestParam(value = "category", required = false) String category) {

        log.debug("Fetching available boosts for company: {}, category: {}", companyId, category);

        try {
            List<AvailableBoostResponse> boosts = dashboardService.getAvailableBoosts(
                    userId,
                    companyId,
                    category
            );

            return ResponseEntity.ok(boosts);

        } catch (Exception e) {
            log.error("Error fetching available boosts for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get complete dashboard overview (aggregates all above)
     * Single endpoint to load entire dashboard
     *
     * GET /api/billing/overview
     *
     * @return DashboardOverviewResponse with all dashboard data
     *
     * Response: Combines all above responses
     * {
     *   "current_plan": {...},
     *   "billing_snapshot": {...},
     *   "usage_metrics": {...},
     *   "notifications": {...},
     *   "available_boosts": {...}
     * }
     *
     * Caching: 30s TTL
     * Cache key: "dashboard_overview:{companyId}"
     *
     * Performance Optimization:
     * - Parallel load all data
     * - Timeout: 1000ms total
     * - Fallback: Return partial data if some calls fail
     */
    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getDashboardOverview(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching dashboard overview for company: {}", companyId);

        try {
            DashboardOverviewResponse overview = dashboardService.getDashboardOverview(userId, companyId);
            return ResponseEntity.ok(overview);

        } catch (Exception e) {
            log.error("Error fetching dashboard overview for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark notification as read
     *
     * PATCH /api/billing/notifications/{notificationId}/read
     */
    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Marking notification {} as read for company: {}", notificationId, companyId);

        try {
            dashboardService.markNotificationAsRead(userId, notificationId, companyId);
            return ResponseEntity.ok().build();

        } catch (com.broadnet.billing.exception.IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error marking notification as read", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete notification
     *
     * DELETE /api/billing/notifications/{notificationId}
     */
    @DeleteMapping("/notifications/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long notificationId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Deleting notification {} for company: {}", notificationId, companyId);

        try {
            dashboardService.deleteNotification(userId, notificationId, companyId);
            return ResponseEntity.noContent().build();

        } catch (IllegalAccessException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error deleting notification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Purchase a boost add-on
     *
     * POST /api/billing/boosts/purchase
     *
     * @param request Purchase request with addon_code
     * @return BoostPurchaseResponse with status
     */
    @PostMapping("/boosts/purchase")
    public ResponseEntity<BoostPurchaseResponse> purchaseBoost(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PurchaseBoostRequest request,
            @RequestHeader(value = "company_id") Long companyId) {

        log.info("Purchasing boost {} for company: {}", request.getAddonCode(), companyId);

        try {
            BoostPurchaseResponse response = dashboardService.purchaseBoost(
                    userId,
                    companyId,
                    request.getAddonCode(),
                    request.getBillingInterval()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid boost purchase for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error purchasing boost for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}