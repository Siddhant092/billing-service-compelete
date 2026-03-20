package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingWebhookEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillingWebhookEventRepository extends JpaRepository<BillingWebhookEvent, Long> {

    boolean existsByStripeEventId(String id);

    Optional<BillingWebhookEvent> findByStripeEventId(String stripeEventId);

    @Query("SELECT w FROM BillingWebhookEvent w WHERE w.processed = false ORDER BY w.createdAt ASC")
    List<BillingWebhookEvent> findUnprocessed();

    @Query("SELECT w FROM BillingWebhookEvent w WHERE w.processed = false AND w.retryCount < :maxRetries ORDER BY w.createdAt ASC")
    List<BillingWebhookEvent> findUnprocessedWithRetryLimit(@Param("maxRetries") Integer maxRetries);

    @Query("SELECT w FROM BillingWebhookEvent w WHERE w.eventType = :eventType ORDER BY w.createdAt DESC")
    Page<BillingWebhookEvent> findByEventType(@Param("eventType") String eventType, Pageable pageable);

    @Query("SELECT w FROM BillingWebhookEvent w WHERE w.stripeSubscriptionId = :subscriptionId ORDER BY w.createdAt DESC")
    List<BillingWebhookEvent> findBySubscriptionId(@Param("subscriptionId") String subscriptionId);

    @Query("SELECT w FROM BillingWebhookEvent w WHERE w.createdAt BETWEEN :startDate AND :endDate ORDER BY w.createdAt DESC")
    List<BillingWebhookEvent> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT w FROM BillingWebhookEvent w WHERE w.errorMessage IS NOT NULL ORDER BY w.createdAt DESC")
    List<BillingWebhookEvent> findFailed();
}
