package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingPlanRepository extends JpaRepository<BillingPlan, Long> {

    Optional<BillingPlan> findByPlanCode(String planCode);

    @Query("SELECT p FROM BillingPlan p WHERE p.isActive = true ORDER BY p.createdAt ASC")
    List<BillingPlan> findAllActivePlans();

    @Query("SELECT p FROM BillingPlan p WHERE p.planCode = :planCode AND p.isActive = true")
    Optional<BillingPlan> findActiveByPlanCode(@Param("planCode") String planCode);

    @Query("SELECT p FROM BillingPlan p WHERE p.isEnterprise = true AND p.isActive = true")
    List<BillingPlan> findAllEnterprisePlans();
}