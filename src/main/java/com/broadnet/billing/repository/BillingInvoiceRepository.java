package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingInvoiceRepository extends JpaRepository<BillingInvoice, Long> {

    Optional<BillingInvoice> findByStripeInvoiceId(String stripeInvoiceId);

    @Query("SELECT i FROM BillingInvoice i WHERE i.companyId = :companyId ORDER BY i.invoiceDate DESC")
    Page<BillingInvoice> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT i FROM BillingInvoice i WHERE i.companyId = :companyId AND i.status IN ('open', 'past_due') ORDER BY i.invoiceDate DESC")
    List<BillingInvoice> findUnpaidByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT i FROM BillingInvoice i WHERE i.invoiceDate BETWEEN :startDate AND :endDate ORDER BY i.invoiceDate DESC")
    List<BillingInvoice> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT i FROM BillingInvoice i WHERE i.status = :status ORDER BY i.invoiceDate DESC")
    Page<BillingInvoice> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT i FROM BillingInvoice i WHERE i.amountDue > i.amountPaid AND i.status IN ('open', 'past_due') ORDER BY i.invoiceDate ASC")
    List<BillingInvoice> findForCollection();
}
