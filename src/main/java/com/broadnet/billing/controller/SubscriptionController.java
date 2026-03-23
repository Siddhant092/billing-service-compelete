package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.ChangePlanRequest;
import com.broadnet.billing.dto.request.CancelSubscriptionRequest;
import com.broadnet.billing.dto.response.AvailablePlanResponse;
import com.broadnet.billing.dto.response.SubscriptionResponse;
import com.broadnet.billing.service.SubscriptionManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Subscription Management API Controller
 * Handles plan changes, cancellations, reactivations
 *
 * Security: Requires authenticated user
 * Rate Limiting: 5 requests/minute per user
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionManagementService subscriptionService;

    /**
     * Get all available plans with their limits
     * Used for: Plan selection UI
     *
     * GET /api/billing/subscription/plans
     *
     * @return List of available plans with limits
     *
     * Response:
     * [
     *   {
     *     "id": 1,
     *     "plan_code": "starter",
     *     "plan_name": "Starter Plan",
     *     "description": "For individuals",
     *     "is_active": true,
     *     "is_enterprise": false,
     *     "support_tier": "basic",
     *     "limits": [
     *       {
     *         "limit_type": "answers_per_period",
     *         "limit_value": 1000,
     *         "billing_interval": "month"
     *       },
     *       ...
     *     ]
     *   },
     *   ...
     * ]
     *
     * Filter:
     * - Only active plans (is_active = true)
     * - Not enterprise plans (is_enterprise = false, unless special param)
     * - Current effective limits only
     *
     * Caching: Cache for 5 minutes (cache key: "available_plans")
     *
     * Performance: < 100ms
     */
    @GetMapping("/plans")
    public ResponseEntity<List<AvailablePlanResponse>> getAvailablePlans(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "include_enterprise", required = false, defaultValue = "false") boolean includeEnterprise) {

        log.debug("Fetching available plans, includeEnterprise: {}", includeEnterprise);

        try {
            List<AvailablePlanResponse> plans = subscriptionService.getAvailablePlans(userId, includeEnterprise);
            log.info("Returned {} available plans", plans.size());
            return ResponseEntity.ok(plans);

        } catch (Exception e) {
            log.error("Error fetching available plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Change current plan to a new plan
     * Handles: Upgrades, downgrades, lateral moves
     *
     * POST /api/billing/subscription/change-plan
     *
     * @param request ChangePlanRequest with new_plan_code
     * @return SubscriptionResponse with updated plan info
     *
     * Request:
     * {
     *   "plan_code": "professional",
     *   "billing_interval": "month"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Plan changed to Professional",
     *   "active_plan_code": "professional",
     *   "previous_plan_code": "starter",
     *   "effective_date": "2025-03-19T00:00:00Z",
     *   "new_limits": {...}
     * }
     *
     * Flow:
     * 1. Get current CompanyBilling
     * 2. Validate new plan exists and is active
     * 3. Update CompanyBilling.activePlanCode with optimistic lock
     * 4. Recalculate effective limits (plan + current addons)
     * 5. Create BillingEntitlementHistory entry
     * 6. Create BillingNotification
     * 7. Sync with Stripe (update subscription)
     *
     * Error Cases:
     * - 400: Plan not found
     * - 400: Same plan as current
     * - 401: Unauthorized
     * - 409: Version conflict (retry)
     * - 500: Database/Stripe error
     *
     * Performance: < 500ms (includes Stripe sync)
     */
    @PostMapping("/change-plan")
    public ResponseEntity<SubscriptionResponse> changePlan(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ChangePlanRequest request,
            @RequestParam(value = "company_id") Long companyId) {

        log.info("Changing plan for company: {}, new plan: {}", companyId, request.getPlanCode());

        try {
            SubscriptionResponse response = subscriptionService.changePlan(
                    userId,
                    companyId,
                    request.getPlanCode(),
                    request.getBillingInterval()
            );

            log.info("Plan changed successfully for company: {}", companyId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid plan change request for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error changing plan for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Cancel subscription (with immediate or end-of-period option)
     *
     * POST /api/billing/subscription/cancel
     *
     * @param request CancelSubscriptionRequest
     * @return SubscriptionResponse confirming cancellation
     *
     * Request:
     * {
     *   "cancel_at_period_end": true,  // false = immediate cancellation
     *   "reason": "Not needed anymore"  // optional feedback
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Subscription canceled",
     *   "cancellation_type": "end_of_period",
     *   "period_end_date": "2025-04-19T00:00:00Z",
     *   "status": "canceled"
     * }
     *
     * Flow:
     * 1. Get CompanyBilling
     * 2. Update subscription status to "canceled"
     * 3. If cancel_at_period_end: Set cancelAtPeriodEnd flag
     * 4. If immediate: Revoke access immediately
     * 5. Create BillingEntitlementHistory
     * 6. Create BillingNotification
     * 7. Sync with Stripe
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 404: No subscription found
     * - 500: Database/Stripe error
     */
    @PostMapping("/cancel")
    public ResponseEntity<SubscriptionResponse> cancelSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CancelSubscriptionRequest request,
            @RequestParam(value = "company_id") Long companyId) {

        log.info("Canceling subscription for company: {}, atPeriodEnd: {}",
                companyId, request.isCancelAtPeriodEnd());

        try {
            SubscriptionResponse response = subscriptionService.cancelSubscription(
                    userId,
                    companyId,
                    request.isCancelAtPeriodEnd(),
                    request.getReason()
            );

            log.info("Subscription canceled for company: {}", companyId);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("Invalid cancellation request for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error canceling subscription for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Reactivate a canceled subscription
     * Only available during grace period (configurable, typically 7-14 days)
     *
     * POST /api/billing/subscription/reactivate
     *
     * @return SubscriptionResponse confirming reactivation
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Subscription reactivated",
     *   "status": "active",
     *   "limits": {...}
     * }
     *
     * Flow:
     * 1. Get canceled CompanyBilling
     * 2. Check if within grace period
     * 3. Update status back to "active"
     * 4. Clear cancellation dates
     * 5. Restore access immediately
     * 6. Create BillingEntitlementHistory
     * 7. Create BillingNotification
     * 8. Sync with Stripe
     *
     * Error Cases:
     * - 400: Not in grace period
     * - 404: Subscription not canceled
     * - 500: Database/Stripe error
     */
    @PostMapping("/reactivate")
    public ResponseEntity<SubscriptionResponse> reactivateSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.info("Reactivating subscription for company: {}", companyId);

        try {
            SubscriptionResponse response = subscriptionService.reactivateSubscription(userId, companyId);

            log.info("Subscription reactivated for company: {}", companyId);
            return ResponseEntity.ok(response);

        } catch (IllegalStateException e) {
            log.warn("Cannot reactivate subscription for company {}: {}", companyId, e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error reactivating subscription for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}