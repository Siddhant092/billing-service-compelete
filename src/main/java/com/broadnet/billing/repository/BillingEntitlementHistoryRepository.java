package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingEntitlementHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingEntitlementHistoryRepository extends JpaRepository<BillingEntitlementHistory, Long> {

    @Query("SELECT h FROM BillingEntitlementHistory h WHERE h.companyId = :companyId ORDER BY h.createdAt DESC")
    Page<BillingEntitlementHistory> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT h FROM BillingEntitlementHistory h WHERE h.companyId = :companyId AND h.changeType = :changeType ORDER BY h.createdAt DESC")
    List<BillingEntitlementHistory> findByCompanyIdAndChangeType(
            @Param("companyId") Long companyId,
            @Param("changeType") String changeType
    );

    @Query("SELECT h FROM BillingEntitlementHistory h WHERE h.triggeredBy = 'webhook' ORDER BY h.createdAt DESC")
    List<BillingEntitlementHistory> findWebhookTriggeredChanges();

    @Query("SELECT h FROM BillingEntitlementHistory h WHERE h.triggeredBy = 'admin' AND h.companyId = :companyId ORDER BY h.createdAt DESC")
    List<BillingEntitlementHistory> findAdminTriggeredChanges(@Param("companyId") Long companyId);

    @Query("SELECT h FROM BillingEntitlementHistory h WHERE h.companyId = :companyId AND h.createdAt BETWEEN :startDate AND :endDate ORDER BY h.createdAt DESC")
    List<BillingEntitlementHistory> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
