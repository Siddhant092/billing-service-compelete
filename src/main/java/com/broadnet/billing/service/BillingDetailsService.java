package com.broadnet.billing.service;

import com.broadnet.billing.dto.request.UpdateAddressRequest;
import com.broadnet.billing.dto.request.UpdatePaymentMethodRequest;
import com.broadnet.billing.dto.response.BillingDetailsResponse;
import com.broadnet.billing.dto.response.BillingPortalResponse;

/**
 * BILLING DETAILS SERVICE
 * Manages billing address and payment methods
 */
public interface BillingDetailsService {

    /**
     * Get current billing details
     *
     * Fetches:
     * - Billing address from BillingPaymentMethod
     * - Default payment method (non-expired)
     * - Company name and tax ID
     *
     * Return: BillingDetailsResponse
     */
    BillingDetailsResponse getBillingDetails(Long companyId);

    /**
     * Update billing address
     *
     * Validates:
     * - All required fields present
     * - Valid country code
     *
     * Updates:
     * - BillingPaymentMethod address fields
     * - Syncs with Stripe customer
     *
     * Return: Updated BillingDetailsResponse
     */
    BillingDetailsResponse updateBillingAddress(
            Long companyId,
            UpdateAddressRequest request
    );

    /**
     * Update default payment method
     *
     * Validates:
     * - Stripe token is valid
     *
     * Operations:
     * 1. Attach payment method to Stripe customer
     * 2. Create BillingPaymentMethod entity
     * 3. If set_as_default: Update previous default
     * 4. Sync with Stripe
     *
     * Return: Updated BillingDetailsResponse
     */
    BillingDetailsResponse updatePaymentMethod(
            Long companyId,
            UpdatePaymentMethodRequest request
    );

    /**
     * Get Stripe customer portal URL
     *
     * User can manage payments, download invoices in portal
     *
     * Return: BillingPortalResponse with portal URL
     */
    BillingPortalResponse getStripePortalUrl(Long companyId);
}

