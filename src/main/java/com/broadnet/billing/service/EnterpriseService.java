package com.broadnet.billing.service;


import com.broadnet.billing.dto.request.CreateEnterpriseContactRequest;
import com.broadnet.billing.dto.response.BillingPeriodResponse;
import com.broadnet.billing.dto.response.EnterpriseContactResponse;
import com.broadnet.billing.dto.response.EnterpriseSummaryResponse;

import java.util.List;

/**
 * ENTERPRISE SERVICE
 * Handles enterprise billing and sales CRM
 */
public interface EnterpriseService {

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
     *
     * Return: EnterpriseContactResponse with ID and status
     */
    EnterpriseContactResponse submitContactRequest(
            CreateEnterpriseContactRequest request,
            Long companyId
    );

    /**
     * Get enterprise customer summary
     *
     * Only for postpaid (enterprise) customers
     *
     * Fetches:
     * - BillingEnterprisePricing (active)
     * - Current period from BillingEnterpriseUsageBilling
     * - Usage and calculated amount
     *
     * Return: EnterpriseSummaryResponse with all details
     *
     * Throws: IllegalStateException if not enterprise customer
     */
    EnterpriseSummaryResponse getEnterpriseSummary(Long companyId);

    /**
     * Get usage-based billing periods (last N periods)
     *
     * Shows:
     * - All BillingEnterpriseUsageBilling records
     * - Usage for each period
     * - Calculated amounts (usage * rates)
     * - Invoice status
     *
     * Sorted: Newest first
     *
     * Return: List of BillingPeriodResponse
     */
    List<BillingPeriodResponse> getBillingPeriods(Long companyId, int limit);
}
