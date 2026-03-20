package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingEnterprisePricing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingEnterprisePricingRepository extends JpaRepository<BillingEnterprisePricing, Long> {
    @Query("SELECT p FROM BillingEnterprisePricing p WHERE p.companyId = :companyId AND p.isActive = true AND p.effectiveFrom <= :asOfDate AND (p.effectiveTo IS NULL OR p.effectiveTo > :asOfDate)")
    Optional<BillingEnterprisePricing> findActivePricingByCompanyId(
            @Param("companyId") Long companyId,
            @Param("asOfDate") LocalDateTime asOfDate
    );

    @Query("SELECT p FROM BillingEnterprisePricing p WHERE p.companyId = :companyId ORDER BY p.effectiveFrom DESC")
    List<BillingEnterprisePricing> findByCompanyIdOrderByEffectiveFromDesc(@Param("companyId") Long companyId);

    @Query("SELECT p FROM BillingEnterprisePricing p WHERE p.isActive = true AND p.effectiveFrom <= :asOfDate AND (p.effectiveTo IS NULL OR p.effectiveTo > :asOfDate)")
    List<BillingEnterprisePricing> findAllActiveEnterprisePricing(@Param("asOfDate") LocalDateTime asOfDate);
}
