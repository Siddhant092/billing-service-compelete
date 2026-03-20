package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingUsageAnalytics;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingUsageAnalyticsRepository extends JpaRepository<BillingUsageAnalytics, Long> {
    @Query("SELECT a FROM BillingUsageAnalytics a WHERE a.companyId = :companyId ORDER BY a.periodStart DESC")
    Page<BillingUsageAnalytics> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT a FROM BillingUsageAnalytics a WHERE a.companyId = :companyId AND a.metricType = :metricType ORDER BY a.periodStart DESC")
    List<BillingUsageAnalytics> findByCompanyIdAndMetric(
            @Param("companyId") Long companyId,
            @Param("metricType") String metricType
    );

    @Query("SELECT a FROM BillingUsageAnalytics a WHERE a.companyId = :companyId AND a.metricType = :metricType AND a.periodType = :periodType ORDER BY a.periodStart DESC")
    List<BillingUsageAnalytics> findByCompanyMetricAndPeriod(
            @Param("companyId") Long companyId,
            @Param("metricType") String metricType,
            @Param("periodType") String periodType
    );

    @Query("SELECT a FROM BillingUsageAnalytics a WHERE a.companyId = :companyId AND a.periodStart BETWEEN :startDate AND :endDate ORDER BY a.periodStart DESC")
    List<BillingUsageAnalytics> findByCompanyIdAndDateRange(
            @Param("companyId") Long companyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
