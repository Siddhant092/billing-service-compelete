package com.broadnet.billing.controller;

import com.broadnet.billing.dto.response.InvoiceResponse;
import com.broadnet.billing.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Invoice Management API Controller
 * Provides invoice viewing and download functionality
 *
 * Security: Requires authenticated user
 * Rate Limiting: 20 requests/minute
 */
@Slf4j
@RestController
@RequestMapping("/api/billing/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Get paginated invoice list for current company
     *
     * GET /api/billing/invoices
     *
     * @param pageable Pagination (default: 20 per page, sorted by invoice_date DESC)
     * @param status Filter by status (optional): draft, open, paid, uncollectible
     * @return Page of InvoiceResponse
     *
     * Response:
     * {
     *   "content": [
     *     {
     *       "id": 1,
     *       "invoice_number": "INV-2025-001",
     *       "stripe_invoice_id": "in_...",
     *       "status": "paid",
     *       "amount_due": 9900,
     *       "amount_paid": 9900,
     *       "total": 9900,
     *       "currency": "usd",
     *       "invoice_date": "2025-03-19T00:00:00Z",
     *       "due_date": "2025-04-19T00:00:00Z",
     *       "paid_at": "2025-03-20T00:00:00Z",
     *       "period_start": "2025-03-19T00:00:00Z",
     *       "period_end": "2025-04-19T00:00:00Z",
     *       "hosted_invoice_url": "https://invoice.stripe.com/...",
     *       "invoice_pdf_url": "https://invoice.stripe.com/...pdf"
     *     },
     *     ...
     *   ],
     *   "total_elements": 12,
     *   "total_pages": 1
     * }
     *
     * Filters:
     * - Company-specific invoices
     * - Optional status filter
     * - Sorted by invoice_date DESC (newest first)
     * - Paginated (20 items default)
     *
     * Caching: No caching (frequently updated)
     */
    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getInvoices(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "company_id") Long companyId,
            @RequestParam(value = "status", required = false) String status,
            Pageable pageable) {

        log.debug("Fetching invoices for company: {}, status: {}, page: {}",
                companyId, status, pageable.getPageNumber());

        try {
            Page<InvoiceResponse> invoices = invoiceService.getInvoices(
                    userId,
                    companyId,
                    status,
                    pageable
            );

            return ResponseEntity.ok(invoices);

        } catch (Exception e) {
            log.error("Error fetching invoices for company: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get specific invoice details
     *
     * GET /api/billing/invoices/{invoiceId}
     *
     * @param invoiceId Invoice ID
     * @return InvoiceResponse with full details
     *
     * Response: Same as list item with additional line_items
     * {
     *   ...invoice fields...,
     *   "line_items": [
     *     {
     *       "description": "Monthly subscription - Professional Plan",
     *       "quantity": 1,
     *       "unit_amount": 9900,
     *       "amount": 9900
     *     },
     *     {
     *       "description": "Answers Boost (Medium) - Monthly addon",
     *       "quantity": 1,
     *       "unit_amount": 2000,
     *       "amount": 2000
     *     }
     *   ],
     *   "subtotal": 11900,
     *   "tax_amount": 1000,
     *   "total": 12900
     * }
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Forbidden (not company's invoice)
     * - 404: Invoice not found
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoiceDetail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long invoiceId,
            @RequestParam(value = "company_id") Long companyId) {

        log.debug("Fetching invoice details for invoiceId: {}, company: {}", invoiceId, companyId);

        try {
            InvoiceResponse invoice = invoiceService.getInvoiceDetail(userId, invoiceId, companyId);
            return ResponseEntity.ok(invoice);

//        } catch (IllegalAccessException e) {
//            log.warn("Unauthorized access to invoice {} by company {}", invoiceId, companyId);
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching invoice details", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Download invoice PDF
     *
     * GET /api/billing/invoices/{invoiceId}/download
     *
     * @param invoiceId Invoice ID
     * @return PDF file as attachment
     *
     * Headers:
     * - Content-Type: application/pdf
     * - Content-Disposition: attachment; filename="invoice-2025-001.pdf"
     *
     * Flow:
     * 1. Fetch invoice from Stripe using hosted URL
     * 2. Return PDF bytes with proper headers
     * 3. Browser automatically downloads
     *
     * Error Cases:
     * - 401: Unauthorized
     * - 403: Forbidden
     * - 404: Invoice not found
     * - 410: PDF no longer available (invoice archived)
     */
    @GetMapping("/{invoiceId}/download")
    public ResponseEntity<?> downloadInvoicePdf(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long invoiceId,
            @RequestParam(value = "company_id") Long companyId) {

        log.info("Downloading PDF for invoice {} by company {}", invoiceId, companyId);

        try {
            byte[] pdfBytes = invoiceService.downloadInvoicePdf(userId, invoiceId, companyId);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/pdf")
                    .header("Content-Disposition", "attachment; filename=\"invoice-" + invoiceId + ".pdf\"")
                    .body(pdfBytes);

//        } catch (IllegalAccessException e) {
//            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error downloading invoice PDF", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}