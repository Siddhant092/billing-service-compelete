package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingPaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BillingPaymentMethodRepository extends JpaRepository<BillingPaymentMethod, Long> {

    Optional<BillingPaymentMethod> findByStripePaymentMethodId(String stripePaymentMethodId);

    @Query("SELECT pm FROM BillingPaymentMethod pm WHERE pm.companyId = :companyId ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    List<BillingPaymentMethod> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT pm FROM BillingPaymentMethod pm WHERE pm.companyId = :companyId AND pm.isDefault = true")
    Optional<BillingPaymentMethod> findDefaultByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT pm FROM BillingPaymentMethod pm WHERE pm.isExpired = true")
    List<BillingPaymentMethod> findExpired();

    @Query("SELECT pm FROM BillingPaymentMethod pm WHERE pm.companyId = :companyId AND pm.isExpired = false ORDER BY pm.isDefault DESC")
    List<BillingPaymentMethod> findActiveByCompanyId(@Param("companyId") Long companyId);
}
