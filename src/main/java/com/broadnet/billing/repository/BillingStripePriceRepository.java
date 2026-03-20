package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingStripePrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingStripePriceRepository extends JpaRepository<BillingStripePrice, Long> {

    Optional<BillingStripePrice> findByStripePriceId(String stripePriceId);

    Optional<BillingStripePrice> findByLookupKey(String lookupKey);

    @Query("SELECT p FROM BillingStripePrice p WHERE p.plan.id = :planId AND p.billingInterval = :interval AND p.isActive = true")
    Optional<BillingStripePrice> findActivePriceByPlanAndInterval(
            @Param("planId") Long planId,
            @Param("interval") BillingStripePrice.BillingInterval interval
    );

    @Query("SELECT p FROM BillingStripePrice p WHERE p.addon.id = :addonId AND p.billingInterval = :interval AND p.isActive = true")
    Optional<BillingStripePrice> findActivePriceByAddonAndInterval(
            @Param("addonId") Long addonId,
            @Param("interval") BillingStripePrice.BillingInterval interval
    );

    @Query("SELECT p FROM BillingStripePrice p WHERE p.plan.id = :planId AND p.isActive = true")
    List<BillingStripePrice> findAllActivePricesByPlan(@Param("planId") Long planId);

    @Query("SELECT p FROM BillingStripePrice p WHERE p.addon.id = :addonId AND p.isActive = true")
    List<BillingStripePrice> findAllActivePricesByAddon(@Param("addonId") Long addonId);
}