package com.pfip.transactionservice.repository;

import com.pfip.transactionservice.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Page<Transaction> findByUserIdAndType(
            Long userId, Transaction.TransactionType type, Pageable pageable);

    Page<Transaction> findByUserIdAndStatus(
            Long userId, Transaction.TransactionStatus status, Pageable pageable);

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    long countByUserId(Long userId);

    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM Transaction t
            WHERE t.userId = :userId
              AND t.type = :type
              AND t.status = 'COMPLETED'
            """)
    BigDecimal sumByUserIdAndType(
            @Param("userId") Long userId,
            @Param("type") Transaction.TransactionType type);

    @Query("""
            SELECT t FROM Transaction t
            WHERE t.userId = :userId
              AND t.createdAt BETWEEN :from AND :to
            ORDER BY t.createdAt DESC
            """)
    Page<Transaction> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);
}
