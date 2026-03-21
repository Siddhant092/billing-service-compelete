package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.UpdatePlanLimitRequest;
import com.broadnet.billing.dto.request.SetEnterprisePricingRequest;
import com.broadnet.billing.dto.request.UpdateEnterpriseContactRequest;
import com.broadnet.billing.dto.response.PlanLimitUpdateResponse;
import com.broadnet.billing.dto.response.EnterprisePricingResponse;
import com.broadnet.billing.dto.response.EnterpriseContactAdminResponse;
import com.broadnet.billing.service.AdminBillingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * Admin Billing API Controller
 * ADMIN ONLY - Manages plans, pricing, and enterprise contacts
 *
 * Security: Requires admin authentication
 * Authorization: Only users with ADMIN role
 * Rate Limiting: 50 requests/minute
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/billing")
@RequiredArgsConstructor
public class AdminBillingController {

    private final AdminBillingService adminService;

    /**
     * Update plan limits (affects all companies on that plan)
     * Creates new limit version with effective date
     *
     * PUT /api/admin/billing/plans/{plan_code}/limits
     *
     * @param planCode Plan code (e.g., "professional")
     * @param request UpdatePlanLimitRequest with new limit values
     * @return PlanLimitUpdateResponse with affected company count
     *
     * Request:
     * {
     *   "limit_type": "answers_per_period",
     *   "limit_value": 10000,
     *   "billing_interval": "month",
     *   "effective_from": "2025-04-01T00:00:00Z"
     * }
     *
     * Response:
     * {
     *   "success": true,
     *   "message": "Plan limit updated. Recomputing entitlements for active subscriptions...",
     *   "affected_companies": 45,
     *   "background_job_id": "job_123"  // Job ID for progress tracking
     * }
     *
     * Flow:
     * 1. Verify admin role
     * 2. Get plan by code (must be active)
     * 3. Create new BillingPlanLimit with effective_from date
     * 4. Find all companies on this plan
     * 5. Trigger async entitlement recalculation job
     * 6. Return response with background job ID
     *
     * Notes:
     * - Change is NOT immediate for active subscriptions
     * - Uses effective_from date (future-dated changes possible)
     * - Old limit becomes effective_to at new limit's effective_from
     * - Entitlement recalculation happens in background
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Not admin
     * - 400: Invalid plan code
     * - 400: Invalid limit value
     * - 409: Duplicate limit (same type/interval/effective_from exists)
     */
    @PutMapping("/plans/{plan_code}/limits")
    public ResponseEntity<PlanLimitUpdateResponse> updatePlanLimits(
            @PathVariable("plan_code") String planCode,
            @Valid @RequestBody UpdatePlanLimitRequest request) {

        log.info("Admin updating limit for plan: {}, limit_type: {}", planCode, request.getLimitType());

        try {
            PlanLimitUpdateResponse response = adminService.updatePlanLimits(
                    planCode,
                    request
            );

            log.info("Plan limit updated, affected {} companies", response.getAffectedCompanies());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid plan limit update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            log.warn("Plan limit update conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            log.error("Error updating plan limits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Set custom enterprise pricing for a company
     *
     * POST /api/admin/enterprise/pricing
     *
     * @param request SetEnterprisePricingRequest with custom rates
     * @return EnterprisePricingResponse confirming setup
     *
     * Request:
     * {
     *   "company_id": 100,
     *   "pricing_tier": "negotiated",
     *   "answers_rate_cents": 50,
     *   "kb_pages_rate_cents": 10,
     *   "agents_rate_cents": 5000,
     *   "users_rate_cents": 2000,
     *   "minimum_monthly_commitment_cents": 100000,
     *   "minimum_answers_commitment": 1000000,
     *   "answers_volume_discount_tiers": [{...}],
     *   "kb_pages_volume_discount_tiers": [{...}],
     *   "effective_from": "2025-04-01T00:00:00Z",
     *   "contract_reference": "ENT-2025-001",
     *   "notes": "Negotiated volume discount"
     * }
     *
     * Response:
     * {
     *   "id": 123,
     *   "success": true,
     *   "message": "Enterprise pricing set successfully",
     *   "pricing_id": 123
     * }
     *
     * Flow:
     * 1. Verify admin role
     * 2. Verify company exists
     * 3. Create BillingEnterprisePricing record
     * 4. Update CompanyBilling.enterprisePricingId
     * 5. Set CompanyBilling.billingMode = postpaid
     * 6. Create BillingNotification for company
     * 7. Log in audit trail
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Not admin
     * - 404: Company not found
     * - 400: Invalid pricing values
     */
    @PostMapping("/enterprise/pricing")
    public ResponseEntity<EnterprisePricingResponse> setEnterprisePricing(
            @Valid @RequestBody SetEnterprisePricingRequest request) {

        log.info("Admin setting enterprise pricing for company: {}", request.getCompanyId());

        try {
            EnterprisePricingResponse response = adminService.setEnterprisePricing(request);

            log.info("Enterprise pricing set for company: {}, pricingId: {}",
                    request.getCompanyId(), response.getPricingId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid enterprise pricing: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error setting enterprise pricing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all enterprise contact requests (CRM-style list)
     *
     * GET /api/admin/enterprise/contacts
     *
     * @param status Filter by status: pending, contacted, in_progress, qualified, closed
     * @param assigned_to Filter by assigned user ID
     * @param pageable Pagination (default: 20 per page)
     * @return Page of EnterpriseContactAdminResponse
     *
     * Response:
     * {
     *   "content": [
     *     {
     *       "id": 789,
     *       "company_id": 100,
     *       "contact_type": "pricing_request",
     *       "name": "John Doe",
     *       "email": "john@company.com",
     *       "phone": "+1-555-1234",
     *       "job_title": "CTO",
     *       "company_name": "Acme Corp",
     *       "company_size": "_201_500",
     *       "status": "in_progress",
     *       "assigned_to": 456,  // User ID of assigned sales rep
     *       "assigned_at": "2025-03-15T10:00:00Z",
     *       "first_contacted_at": "2025-03-16T14:00:00Z",
     *       "message": "We need custom pricing",
     *       "estimated_usage": {...},
     *       "budget_range": "$50K-$100K/month",
     *       "outcome": null,
     *       "notes": "Following up on Wednesday call",
     *       "created_at": "2025-03-10T08:00:00Z"
     *     },
     *     ...
     *   ],
     *   "total_elements": 45,
     *   "total_pages": 3
     * }
     *
     * Filters:
     * - Optional status filter
     * - Optional assigned user filter
     * - Sorted by created_at DESC (newest first)
     * - Paginated (20 per page)
     *
     * Use Cases:
     * - Sales team CRM view of all leads
     * - Filter by status to show pipeline
     * - Filter by assigned user to show individual's leads
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Not admin
     */
    @GetMapping("/enterprise/contacts")
    public ResponseEntity<Page<EnterpriseContactAdminResponse>> getEnterpriseContacts(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "assigned_to", required = false) Long assignedTo,
            Pageable pageable) {

        log.debug("Fetching enterprise contacts, status: {}, assigned_to: {}", status, assignedTo);

        try {
            Page<EnterpriseContactAdminResponse> contacts = adminService.getEnterpriseContacts(
                    status,
                    assignedTo,
                    pageable
            );

            return ResponseEntity.ok(contacts);

        } catch (Exception e) {
            log.error("Error fetching enterprise contacts", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Update enterprise contact status and notes
     * Sales team uses this to track lead progress
     *
     * PATCH /api/admin/enterprise/contacts/{contactId}
     *
     * @param contactId Contact ID
     * @param request UpdateEnterpriseContactRequest
     * @return Updated EnterpriseContactAdminResponse
     *
     * Request:
     * {
     *   "status": "qualified",
     *   "assigned_to": 456,  // User ID to assign to
     *   "outcome": null,
     *   "notes": "Customer qualified, pricing negotiation in progress"
     * }
     *
     * Flow:
     * 1. Get contact by ID
     * 2. Update status
     * 3. If assigned_to changed: Update assignedAt timestamp
     * 4. If outcome set: Update closedAt timestamp
     * 5. Log changes
     * 6. Create notification if status changed
     *
     * Status Transitions:
     * - pending → contacted
     * - contacted → in_progress
     * - in_progress → qualified
     * - qualified → closed OR rejected
     * - Any → closed
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Not admin
     * - 404: Contact not found
     * - 400: Invalid status transition
     */
    @PatchMapping("/enterprise/contacts/{contactId}")
    public ResponseEntity<EnterpriseContactAdminResponse> updateEnterpriseContact(
            @PathVariable Long contactId,
            @Valid @RequestBody UpdateEnterpriseContactRequest request) {

        log.info("Admin updating enterprise contact {}, new status: {}", contactId, request.getStatus());

        try {
            EnterpriseContactAdminResponse response = adminService.updateEnterpriseContact(
                    contactId,
                    request
            );

            log.info("Enterprise contact {} updated", contactId);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid contact update: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error updating enterprise contact", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}