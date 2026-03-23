package com.broadnet.billing.controller;

import com.broadnet.billing.dto.request.CreateEnterpriseContactRequest;
import com.broadnet.billing.dto.response.EnterpriseContactResponse;
import com.broadnet.billing.dto.response.EnterpriseSummaryResponse;
import com.broadnet.billing.dto.response.BillingPeriodResponse;
import com.broadnet.billing.service.EnterpriseService;
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
 * Enterprise Billing API Controller
 * Handles enterprise customer inquiries and usage-based billing queries
 *
 * Security: Requires authenticated user
 * Rate Limiting: 20 requests/minute
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseService enterpriseService;

    /**
     * Submit enterprise inquiry/contact request
     * Used by companies interested in custom enterprise pricing
     *
     * POST /api/billing/enterprise/contact
     *
     * @param request CreateEnterpriseContactRequest with company info
     * @return EnterpriseContactResponse confirming submission
     *
     * Request:
     * {
     *   "contact_type": "pricing_request",  // enterprise_inquiry, pricing_request, custom_plan_request, support_request
     *   "name": "John Doe",
     *   "email": "john@company.com",
     *   "phone": "+1-555-1234",
     *   "job_title": "CTO",
     *   "company_name": "Acme Corp",
     *   "company_size": "_201_500",  // _1_10, _11_50, _51_200, _201_500, _501_1000, _1000_plus
     *   "message": "We need custom pricing for our organization",
     *   "estimated_usage": {
     *     "answers_per_month": 500000,
     *     "kb_pages": 10000,
     *     "agents": 50,
     *     "users": 200
     *   },
     *   "budget_range": "$50,000 - $100,000 per month",
     *   "preferred_contact_method": "email",  // email, phone, video_call
     *   "preferred_contact_time": "9AM-5PM PT"
     * }
     *
     * Response:
     * {
     *   "id": 1,
     *   "status": "pending",
     *   "message": "Your inquiry has been submitted. Our sales team will contact you within 24 hours."
     * }
     *
     * Flow:
     * 1. Create BillingEnterpriseContact record
     * 2. Set status to "pending"
     * 3. Create notification for sales team
     * 4. Send confirmation email to contact
     * 5. Return response
     *
     * Error Cases:
     * - 400: Invalid contact info
     * - 500: Email sending error
     */
    @PostMapping("/contact")
    public ResponseEntity<EnterpriseContactResponse> submitEnterpriseContact(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateEnterpriseContactRequest request,
            @RequestParam(value = "company_id", required = false) Long companyId) {

        log.info("Submitting enterprise contact request from: {}", request.getEmail());

        try {
            EnterpriseContactResponse response = enterpriseService.submitContactRequest(
                    userId,
                    request,
                    companyId
            );

            log.info("Enterprise contact submitted, contact ID: {}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid enterprise contact request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error submitting enterprise contact request", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get enterprise customer summary
     * Only available if company has enterprise billing enabled
     *
     * GET /api/billing/enterprise/summary
     *
     * @return EnterpriseSummaryResponse with billing info
     *
     * Response:
     * {
     *   "billing_mode": "postpaid",
     *   "pricing_tier": "negotiated",
     *   "annual_commitment": 500000,  // cents
     *   "monthly_minimum": 40000,  // cents
     *   "contract_reference": "ENT-2025-001",
     *   "rates": {
     *     "answers_rate_cents": 50,
     *     "kb_pages_rate_cents": 10,
     *     "agents_rate_cents": 5000,
     *     "users_rate_cents": 2000
     *   },
     *   "current_period": {
     *     "start": "2025-01-01T00:00:00Z",
     *     "end": "2025-02-01T00:00:00Z",
     *     "usage": {
     *       "answers": 2500000,
     *       "kb_pages": 15000,
     *       "agents": 45,
     *       "users": 180
     *     },
     *     "calculated_amount": 375000  // cents
     *   }
     * }
     *
     * Filter:
     * - Only for postpaid (enterprise) customers
     * - Current period usage only
     * - Active pricing only
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Not an enterprise customer
     * - 404: No enterprise pricing found
     */
    @GetMapping("/summary")
    public ResponseEntity<EnterpriseSummaryResponse> getEnterpriseSummary(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching enterprise summary for company: {}", companyId);

        try {
            EnterpriseSummaryResponse summary = enterpriseService.getEnterpriseSummary(userId, companyId);
            return ResponseEntity.ok(summary);

        } catch (IllegalStateException e) {
            log.warn("Company {} is not an enterprise customer", companyId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error fetching enterprise summary for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get usage-based billing periods and amounts
     * Shows calculation history for billing periods
     *
     * GET /api/billing/enterprise/billing-periods
     *
     * @param limit Number of periods to return (default: 12, max: 24)
     * @return List of BillingPeriodResponse
     *
     * Response:
     * [
     *   {
     *     "period_number": 3,
     *     "period_start": "2025-02-01T00:00:00Z",
     *     "period_end": "2025-03-01T00:00:00Z",
     *     "status": "calculated",  // pending, calculated, invoiced, paid
     *     "usage": {
     *       "answers": 2300000,
     *       "kb_pages": 14500,
     *       "agents": 42,
     *       "users": 175
     *     },
     *     "amounts": {
     *       "answers": 115000,  // cents
     *       "kb_pages": 145000,
     *       "agents": 210000,
     *       "users": 350000
     *     },
     *     "subtotal": 820000,
     *     "tax": 65600,
     *     "total": 885600,
     *     "invoice_id": 5,
     *     "stripe_invoice_id": "in_...",
     *     "paid": false
     *   },
     *   ...
     * ]
     *
     * Filter:
     * - Only for enterprise customers
     * - Paginated by period (newest first)
     * - Include all calculation details
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Not an enterprise customer
     */
    @GetMapping("/billing-periods")
    public ResponseEntity<List<BillingPeriodResponse>> getBillingPeriods(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId,
            @RequestParam(value = "limit", required = false, defaultValue = "12") int limit) {

        log.debug("Fetching billing periods for company: {}, limit: {}", companyId, limit);

        try {
            List<BillingPeriodResponse> periods = enterpriseService.getBillingPeriods(
                    userId,
                    companyId,
                    Math.min(limit, 24)  // Cap at 24 for performance
            );

            return ResponseEntity.ok(periods);

        } catch (IllegalStateException e) {
            log.warn("Company {} is not an enterprise customer", companyId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (Exception e) {
            log.error("Error fetching billing periods for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}