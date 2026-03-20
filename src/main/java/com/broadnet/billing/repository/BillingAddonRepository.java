package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingAddon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingAddonRepository extends JpaRepository<BillingAddon, Long> {

    Optional<BillingAddon> findByAddonCode(String addonCode);

    @Query("SELECT a FROM BillingAddon a WHERE a.isActive = true ORDER BY a.category ASC, a.tier ASC")
    List<BillingAddon> findAllActiveAddons();

    @Query("SELECT a FROM BillingAddon a WHERE a.category = :category AND a.isActive = true ORDER BY a.tier ASC")
    List<BillingAddon> findActiveByCategory(@Param("category") String category);

    @Query("SELECT a FROM BillingAddon a WHERE a.category = :category AND a.tier = :tier AND a.isActive = true")
    Optional<BillingAddon> findByCategoryAndTier(
            @Param("category") String category,
            @Param("tier") String tier
    );
}
