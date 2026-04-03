package com.pfip.transactionservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_transaction_user_id", columnList = "user_id"),
        @Index(name = "idx_transaction_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "txn_seq")
    @SequenceGenerator(name = "txn_seq", sequenceName = "transaction_sequence", allocationSize = 1)
    private Long id;

    /**
     * The user who owns this transaction (resolved from the JWT subject
     * by the gateway; stored as a plain Long so this service stays decoupled
     * from the user-service database).
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    @Column(length = 255)
    private String description;

    /** External reference number (e.g. bank ref, payment-gateway ref). */
    @Column(name = "reference_number", unique = true, length = 100)
    private String referenceNumber;

    /** Category tag for analytics (e.g. FOOD, UTILITIES, SALARY). */
    @Column(length = 50)
    private String category;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public enum TransactionType {
        CREDIT, DEBIT
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, REVERSED
    }
}
