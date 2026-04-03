package com.pfip.transactionservice.dto;

import com.pfip.transactionservice.entity.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateTransactionRequest {

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        @Digits(integer = 15, fraction = 4, message = "Invalid amount format")
        private BigDecimal amount;

        @NotNull(message = "Transaction type is required")
        private Transaction.TransactionType type;

        @Size(max = 3)
        @Builder.Default
        private String currency = "USD";

        @Size(max = 255)
        private String description;

        @Size(max = 100)
        private String referenceNumber;

        @Size(max = 50)
        private String category;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionResponse {
        private Long id;
        private Long userId;
        private BigDecimal amount;
        private String type;
        private String status;
        private String currency;
        private String description;
        private String referenceNumber;
        private String category;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionSummary {
        private Long userId;
        private BigDecimal totalCredits;
        private BigDecimal totalDebits;
        private BigDecimal netBalance;
        private long transactionCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PagedResponse<T> {
        private java.util.List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean last;
    }
}
