package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingUsageLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingUsageLogRepository extends JpaRepository<BillingUsageLog, Long> {
    @Query("SELECT ul FROM BillingUsageLog ul WHERE ul.companyId = :companyId ORDER BY ul.createdAt DESC")
    Page<BillingUsageLog> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT ul FROM BillingUsageLog ul WHERE ul.companyId = :companyId AND ul.usageType = :usageType ORDER BY ul.createdAt DESC")
    Page<BillingUsageLog> findByCompanyIdAndType(
            @Param("companyId") Long companyId,
            @Param("usageType") String usageType,
            Pageable pageable
    );

    @Query("SELECT ul FROM BillingUsageLog ul WHERE ul.wasBlocked = true ORDER BY ul.createdAt DESC")
    List<BillingUsageLog> findBlocked();

    @Query("SELECT ul FROM BillingUsageLog ul WHERE ul.companyId = :companyId AND ul.wasBlocked = true ORDER BY ul.createdAt DESC")
    List<BillingUsageLog> findBlockedByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT ul FROM BillingUsageLog ul WHERE ul.companyId = :companyId AND ul.createdAt BETWEEN :startDate AND :endDate ORDER BY ul.createdAt DESC")
    List<BillingUsageLog> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
