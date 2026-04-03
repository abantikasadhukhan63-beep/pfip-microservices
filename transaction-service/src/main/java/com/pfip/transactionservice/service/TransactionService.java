package com.pfip.transactionservice.service;

import com.pfip.common.exception.DuplicateResourceException;
import com.pfip.common.exception.ResourceNotFoundException;
import com.pfip.transactionservice.dto.TransactionDtos;
import com.pfip.transactionservice.entity.Transaction;
import com.pfip.transactionservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    // ── Create ────────────────────────────────────────────────────────────────

    @Transactional
    public TransactionDtos.TransactionResponse create(Long userId,
                                                      TransactionDtos.CreateTransactionRequest req) {
        // Deduplicate on caller-supplied reference number
        if (req.getReferenceNumber() != null) {
            transactionRepository.findByReferenceNumber(req.getReferenceNumber())
                    .ifPresent(t -> {
                        throw new DuplicateResourceException(
                                "Transaction", "referenceNumber", req.getReferenceNumber());
                    });
        }

        Transaction txn = Transaction.builder()
                .userId(userId)
                .amount(req.getAmount())
                .type(req.getType())
                .currency(req.getCurrency() != null ? req.getCurrency() : "USD")
                .description(req.getDescription())
                .referenceNumber(req.getReferenceNumber() != null
                        ? req.getReferenceNumber()
                        : UUID.randomUUID().toString())
                .category(req.getCategory())
                .status(Transaction.TransactionStatus.COMPLETED)
                .processedAt(LocalDateTime.now())
                .build();

        Transaction saved = transactionRepository.save(txn);
        log.info("Created transaction {} for user {}", saved.getId(), userId);
        return toResponse(saved);
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TransactionDtos.TransactionResponse getById(Long userId, Long txnId) {
        Transaction txn = transactionRepository.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", txnId));
        // Users can only see their own transactions
        if (!txn.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction", "id", txnId);
        }
        return toResponse(txn);
    }

    @Transactional(readOnly = true)
    public TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse> getAllForUser(
            Long userId, Pageable pageable) {

        Page<Transaction> page = transactionRepository.findByUserId(userId, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse> getByType(
            Long userId, Transaction.TransactionType type, Pageable pageable) {

        Page<Transaction> page = transactionRepository.findByUserIdAndType(userId, type, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse> getByDateRange(
            Long userId, LocalDateTime from, LocalDateTime to, Pageable pageable) {

        Page<Transaction> page = transactionRepository
                .findByUserIdAndDateRange(userId, from, to, pageable);
        return toPagedResponse(page);
    }

    @Transactional(readOnly = true)
    public TransactionDtos.TransactionSummary getSummary(Long userId) {
        BigDecimal credits = transactionRepository
                .sumByUserIdAndType(userId, Transaction.TransactionType.CREDIT);
        BigDecimal debits  = transactionRepository
                .sumByUserIdAndType(userId, Transaction.TransactionType.DEBIT);
        long count = transactionRepository.countByUserId(userId);

        return TransactionDtos.TransactionSummary.builder()
                .userId(userId)
                .totalCredits(credits)
                .totalDebits(debits)
                .netBalance(credits.subtract(debits))
                .transactionCount(count)
                .build();
    }

    // ── Update status ─────────────────────────────────────────────────────────

    @Transactional
    public TransactionDtos.TransactionResponse updateStatus(Long userId,
                                                            Long txnId,
                                                            Transaction.TransactionStatus newStatus) {
        Transaction txn = transactionRepository.findById(txnId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", txnId));
        if (!txn.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Transaction", "id", txnId);
        }
        txn.setStatus(newStatus);
        if (newStatus == Transaction.TransactionStatus.COMPLETED) {
            txn.setProcessedAt(LocalDateTime.now());
        }
        return toResponse(transactionRepository.save(txn));
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private TransactionDtos.TransactionResponse toResponse(Transaction t) {
        return TransactionDtos.TransactionResponse.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .amount(t.getAmount())
                .type(t.getType().name())
                .status(t.getStatus().name())
                .currency(t.getCurrency())
                .description(t.getDescription())
                .referenceNumber(t.getReferenceNumber())
                .category(t.getCategory())
                .createdAt(t.getCreatedAt())
                .processedAt(t.getProcessedAt())
                .build();
    }

    private TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse> toPagedResponse(
            Page<Transaction> page) {
        return TransactionDtos.PagedResponse.<TransactionDtos.TransactionResponse>builder()
                .content(page.getContent().stream().map(this::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
