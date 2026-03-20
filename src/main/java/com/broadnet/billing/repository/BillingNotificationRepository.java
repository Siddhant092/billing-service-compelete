package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillingNotificationRepository extends JpaRepository<BillingNotification, Long> {

    @Query("SELECT n FROM BillingNotification n WHERE n.companyId = :companyId AND n.isRead = false ORDER BY n.createdAt DESC")
    Page<BillingNotification> findUnreadByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT n FROM BillingNotification n WHERE n.companyId = :companyId ORDER BY n.createdAt DESC")
    Page<BillingNotification> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT n FROM BillingNotification n WHERE n.notificationType = :type ORDER BY n.createdAt DESC")
    Page<BillingNotification> findByType(@Param("type") String type, Pageable pageable);

    @Query("SELECT n FROM BillingNotification n WHERE n.expiresAt IS NOT NULL AND n.expiresAt <= :asOfDate")
    List<BillingNotification> findExpired(@Param("asOfDate") LocalDateTime asOfDate);

    @Query("SELECT n FROM BillingNotification n WHERE n.stripeEventId = :stripeEventId")
    List<BillingNotification> findByStripeEventId(@Param("stripeEventId") String stripeEventId);
}
