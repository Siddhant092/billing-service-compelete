package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingPlanLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingPlanLimitRepository extends JpaRepository<BillingPlanLimit, Long> {
    @Query("SELECT l FROM BillingPlanLimit l WHERE l.plan.id = :planId AND l.isActive = true ORDER BY l.limitType ASC")
    List<BillingPlanLimit> findActiveLimitsByPlanId(@Param("planId") Long planId);

    @Query("SELECT l FROM BillingPlanLimit l WHERE l.plan.id = :planId AND l.limitType = :limitType AND l.isActive = true AND l.effectiveFrom <= :asOfDate AND (l.effectiveTo IS NULL OR l.effectiveTo > :asOfDate)")
    Optional<BillingPlanLimit> findActiveLimitByPlanAndType(
            @Param("planId") Long planId,
            @Param("limitType") String limitType,
            @Param("asOfDate") LocalDateTime asOfDate
    );

    @Query("SELECT l FROM BillingPlanLimit l WHERE l.plan.id = :planId AND l.effectiveFrom <= :date AND (l.effectiveTo IS NULL OR l.effectiveTo > :date)")
    List<BillingPlanLimit> findLimitsEffectiveAt(
            @Param("planId") Long planId,
            @Param("date") LocalDateTime date
    );

    @Query("SELECT l FROM BillingPlanLimit l WHERE l.plan.id = :planId ORDER BY l.effectiveFrom DESC")
    List<BillingPlanLimit> findAllByPlanIdOrderByEffectiveFromDesc(@Param("planId") Long planId);
}
