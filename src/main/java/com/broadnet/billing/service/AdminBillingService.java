package com.broadnet.billing.service;


import com.broadnet.billing.dto.request.SetEnterprisePricingRequest;
import com.broadnet.billing.dto.request.UpdateEnterpriseContactRequest;
import com.broadnet.billing.dto.request.UpdatePlanLimitRequest;
import com.broadnet.billing.dto.response.EnterpriseContactAdminResponse;
import com.broadnet.billing.dto.response.EnterprisePricingResponse;
import com.broadnet.billing.dto.response.PlanLimitUpdateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * ADMIN BILLING SERVICE
 * ADMIN ONLY operations for plan and pricing management
 */
public interface AdminBillingService {

    /**
     * Update plan limits (affects all companies on plan)
     *
     * Creates:
     * - New BillingPlanLimit with effective_from date
     * - Old limit gets effective_to = new limit's effective_from
     *
     * Triggers:
     * - Background job to recalculate entitlements for affected companies
     * - Job finds all CompanyBilling with this plan
     * - Recalculates effective limits for each
     *
     * Return: PlanLimitUpdateResponse with affected count and job ID
     */
    PlanLimitUpdateResponse updatePlanLimits(
            String planCode,
            UpdatePlanLimitRequest request
    );

    /**
     * Set custom enterprise pricing for company
     *
     * Creates:
     * - BillingEnterprisePricing with all custom rates
     *
     * Updates:
     * - CompanyBilling.enterprisePricingId
     * - CompanyBilling.billingMode = "postpaid"
     *
     * Creates:
     * - BillingNotification for company
     * - Audit log entry
     *
     * Return: EnterprisePricingResponse with pricing ID
     */
    EnterprisePricingResponse setEnterprisePricing(
            SetEnterprisePricingRequest request
    );

    /**
     * Get enterprise contact requests (CRM view)
     *
     * Filters:
     * - Optional: status (pending, contacted, in_progress, qualified, closed)
     * - Optional: assigned_to (user ID)
     *
     * Sort: by created_at DESC (newest first)
     *
     * Return: Paginated list of contacts for sales team
     */
    Page<EnterpriseContactAdminResponse> getEnterpriseContacts(
            String status,
            Long assignedTo,
            Pageable pageable
    );

    /**
     * Update enterprise contact (sales team tracking)
     *
     * Updates:
     * - status (with validation of state transitions)
     * - assigned_to (sales rep assignment)
     * - outcome (won, lost, null)
     * - notes
     *
     * Updates timestamps:
     * - assignedAt when assigned changes
     * - closedAt when outcome is set
     *
     * Return: Updated EnterpriseContactAdminResponse
     */
    EnterpriseContactAdminResponse updateEnterpriseContact(
            Long contactId,
            UpdateEnterpriseContactRequest request
    );
}