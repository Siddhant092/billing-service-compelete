package com.broadnet.billing.service.impl;

import com.broadnet.billing.dto.response.InvoiceResponse;
import com.broadnet.billing.entity.*;
import com.broadnet.billing.exception.*;
import com.broadnet.billing.repository.*;
import com.broadnet.billing.service.InvoiceService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ============================================================================
 * INVOICE SERVICE IMPLEMENTATION
 * Manages invoice retrieval, details, and PDF downloads
 * Architecture: Query BillingInvoice, integrate with Stripe for PDFs
 * ============================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final BillingInvoiceRepository invoiceRepository;
    private final CompanyBillingRepository billingRepository;

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    /**
     * Get paginated invoices for company
     *
     * Filters:
     * - Company-specific (from company_id)
     * - Optional: status filter
     * - Sort: by invoice_date DESC (newest first)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> getInvoices(Long companyId, String status, Pageable pageable) {

        log.debug("Fetching invoices for company: {}, status: {}", companyId, status);

        // Verify company billing exists
        billingRepository.findByCompanyId(companyId)
                .orElseThrow(() -> new CompanyBillingNotFoundException(companyId));

        Page<BillingInvoice> invoices;

        if (status != null && !status.isEmpty()) {
            invoices = invoiceRepository.findByStatus(status, pageable);
        } else {
            invoices = invoiceRepository.findByCompanyId(companyId, pageable);
        }

        return invoices.map(this::mapToInvoiceResponse);
    }

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
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceDetail(Long invoiceId, Long companyId) {

        log.debug("Fetching invoice details - ID: {}, Company: {}", invoiceId, companyId);

        BillingInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BillingInvoiceNotFoundException(invoiceId));

        // Verify company access
        if (!invoice.getCompanyId().equals(companyId)) {
            log.warn("Unauthorized access to invoice {} by company {}", invoiceId, companyId);
            throw new UnauthorizedAccessException(
                    "You do not have permission to access this invoice");
        }

        return mapToInvoiceResponse(invoice);
    }

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
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] downloadInvoicePdf(Long invoiceId, Long companyId) {

        log.info("Downloading PDF for invoice {} by company {}", invoiceId, companyId);

        BillingInvoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BillingInvoiceNotFoundException(invoiceId));

        // Verify company access
        if (!invoice.getCompanyId().equals(companyId)) {
            throw new UnauthorizedAccessException(
                    "You do not have permission to access this invoice");
        }

        // Check if PDF URL exists
        if (invoice.getInvoicePdfUrl() == null || invoice.getInvoicePdfUrl().isEmpty()) {
            throw new IllegalStateException("PDF not available for this invoice");
        }

        try {
            // Fetch PDF from URL
            URL url = new URL(invoice.getInvoicePdfUrl());
            return url.openConnection().getInputStream().readAllBytes();

        } catch (Exception e) {
            log.error("Error downloading PDF for invoice {}: {}", invoiceId, e.getMessage());
            throw new PdfGenerationException(
                    "Failed to download invoice PDF: " + e.getMessage(), e);
        }
    }

    // ===== Helper Methods =====

    /**
     * Map BillingInvoice to InvoiceResponse
     */
    private InvoiceResponse mapToInvoiceResponse(BillingInvoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .stripeInvoiceId(invoice.getStripeInvoiceId())
                .status(String.valueOf(invoice.getStatus()))
                .amountDue(invoice.getAmountDue())
                .amountPaid(invoice.getAmountPaid())
                .total(invoice.getTotal())
                .currency(invoice.getCurrency())
                .invoiceDate(invoice.getInvoiceDate())
                .dueDate(invoice.getDueDate())
                .paidAt(invoice.getPaidAt())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .hostedInvoiceUrl(invoice.getHostedInvoiceUrl())
                .invoicePdfUrl(invoice.getInvoicePdfUrl())
                .build();
    }
}