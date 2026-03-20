package com.broadnet.billing.repository;

import com.broadnet.billing.entity.BillingEnterpriseContact;
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
public interface BillingEnterpriseContactRepository extends JpaRepository<BillingEnterpriseContact, Integer> {


    @Query("SELECT c FROM BillingEnterpriseContact c WHERE c.status = :status ORDER BY c.createdAt DESC")
    Page<BillingEnterpriseContact> findByStatus(@Param("status") String status, Pageable pageable);

    @Query("SELECT c FROM BillingEnterpriseContact c WHERE c.status IN ('pending', 'contacted', 'in_progress') ORDER BY c.createdAt ASC")
    List<BillingEnterpriseContact> findPendingContacts();

    @Query("SELECT c FROM BillingEnterpriseContact c WHERE c.assignedTo = :userId ORDER BY c.status ASC, c.createdAt DESC")
    List<BillingEnterpriseContact> findAssignedTo(@Param("userId") Long userId);

    @Query("SELECT c FROM BillingEnterpriseContact c WHERE c.outcome = :outcome ORDER BY c.closedAt DESC")
    Page<BillingEnterpriseContact> findByOutcome(@Param("outcome") String outcome, Pageable pageable);

    @Query("SELECT c FROM BillingEnterpriseContact c WHERE c.companyId = :companyId ORDER BY c.createdAt DESC")
    List<BillingEnterpriseContact> findByCompanyId(@Param("companyId") Long companyId);

    @Query("SELECT c FROM BillingEnterpriseContact c WHERE c.createdAt BETWEEN :startDate AND :endDate ORDER BY c.createdAt DESC")
    Page<BillingEnterpriseContact> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    Optional<BillingEnterpriseContact> findById(Long contactId);
}
