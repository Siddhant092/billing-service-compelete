package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingEnterpriseUsageBilling;
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
public interface BillingEnterpriseUsageBillingRepository extends JpaRepository<BillingEnterpriseUsageBilling, Long> {
    @Query("SELECT b FROM BillingEnterpriseUsageBilling b WHERE b.companyId = :companyId AND b.billingPeriodStart = :periodStart AND b.billingPeriodEnd = :periodEnd")
    Optional<BillingEnterpriseUsageBilling> findByCompanyAndPeriod(
            @Param("companyId") Long companyId,
            @Param("periodStart") LocalDateTime periodStart,
            @Param("periodEnd") LocalDateTime periodEnd
    );

    @Query("SELECT b FROM BillingEnterpriseUsageBilling b WHERE b.companyId = :companyId ORDER BY b.billingPeriodEnd DESC")
    Page<BillingEnterpriseUsageBilling> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT b FROM BillingEnterpriseUsageBilling b WHERE b.billingStatus = 'pending' ORDER BY b.billingPeriodEnd ASC")
    List<BillingEnterpriseUsageBilling> findPendingCalculation();

    @Query("SELECT b FROM BillingEnterpriseUsageBilling b WHERE b.billingStatus = 'calculated' ORDER BY b.billingPeriodEnd ASC")
    List<BillingEnterpriseUsageBilling> findCalculatedNotInvoiced();

    @Query("SELECT b FROM BillingEnterpriseUsageBilling b WHERE b.billingStatus = :status ORDER BY b.billingPeriodEnd DESC")
    Page<BillingEnterpriseUsageBilling> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT b FROM BillingEnterpriseUsageBilling b WHERE b.invoiceId = :invoiceId")
    Optional<BillingEnterpriseUsageBilling> findByInvoiceId(@Param("invoiceId") Long invoiceId);
}
