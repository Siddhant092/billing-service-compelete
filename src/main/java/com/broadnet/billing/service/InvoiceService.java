package com.broadnet.billing.service;


import com.broadnet.billing.dto.response.InvoiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * INVOICE SERVICE
 * Manages invoice retrieval and downloads
 */
public interface InvoiceService {

    /**
     * Get paginated invoices for company
     *
     * Filters:
     * - Company-specific (from company_id)
     * - Optional: status filter
     * - Sort: by invoice_date DESC (newest first)
     *
     * Return: Paginated InvoiceResponse list
     */
    Page<InvoiceResponse> getInvoices(Long companyId, String status, Pageable pageable);

    /**
     * Get specific invoice with line items
     *
     * Validates:
     * - Invoice exists
     * - Belongs to this company
     *
     * Includes:
     * - All invoice fields
     * - Line items (plan + addons)
     * - Subtotal, tax, total
     * - Payment status
     *
     * Return: InvoiceResponse with full details
     */
    InvoiceResponse getInvoiceDetail(Long invoiceId, Long companyId);

    /**
     * Download invoice PDF
     *
     * Validates:
     * - Invoice exists
     * - Belongs to this company
     *
     * Fetches PDF from Stripe using hosted_invoice_url
     *
     * Return: PDF bytes for download
     *
     * Throws: IllegalAccessException if not company's invoice
     */
    byte[] downloadInvoicePdf(Long invoiceId, Long companyId);
}

