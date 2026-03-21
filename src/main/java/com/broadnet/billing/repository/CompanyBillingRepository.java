package com.broadnet.billing.repository;

import com.broadnet.billing.entity.CompanyBilling;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyBillingRepository extends JpaRepository<CompanyBilling, Long> {

    Optional<CompanyBilling> findByCompanyId(Long companyId);

    Optional<CompanyBilling> findByStripeCustomerId(String stripeCustomerId);

    Optional<CompanyBilling> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.subscriptionStatus = 'active'")
    List<CompanyBilling> findAllActiveSubscriptions();

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.subscriptionStatus = 'past_due' OR cb.paymentFailureDate IS NOT NULL")
    List<CompanyBilling> findPastDueSubscriptions();

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.answersBlocked = true")
    List<CompanyBilling> findAnswersBlocked();

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.serviceRestrictedAt IS NOT NULL AND cb.serviceRestrictedAt <= :asOfDate")
    List<CompanyBilling> findServiceRestricted(@Param("asOfDate") LocalDateTime asOfDate);

    @Query("SELECT cb FROM CompanyBilling cb WHERE cb.billingMode = 'postpaid' AND cb.enterprisePricingId IS NOT NULL")
    List<CompanyBilling> findEnterpriseCustomers();

}
