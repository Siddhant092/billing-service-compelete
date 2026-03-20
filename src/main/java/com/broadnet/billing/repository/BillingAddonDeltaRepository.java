package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingAddonDelta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingAddonDeltaRepository extends JpaRepository<BillingAddonDelta, Long> {

    @Query("SELECT d FROM BillingAddonDelta d WHERE d.addon.id = :addonId AND d.isActive = true ORDER BY d.effectiveFrom DESC")
    List<BillingAddonDelta> findActiveDeltasByAddonId(@Param("addonId") Long addonId);

    @Query("SELECT d FROM BillingAddonDelta d WHERE d.addon.id = :addonId AND d.deltaType = :deltaType AND d.isActive = true AND d.effectiveFrom <= :asOfDate AND (d.effectiveTo IS NULL OR d.effectiveTo > :asOfDate)")
    Optional<BillingAddonDelta> findActiveByAddonAndType(
            @Param("addonId") Long addonId,
            @Param("deltaType") String deltaType,
            @Param("asOfDate") LocalDateTime asOfDate
    );

    @Query("SELECT d FROM BillingAddonDelta d WHERE d.addon.id = :addonId ORDER BY d.effectiveFrom DESC")
    List<BillingAddonDelta> findAllByAddonIdOrderByEffectiveFromDesc(@Param("addonId") Long addonId);
}
