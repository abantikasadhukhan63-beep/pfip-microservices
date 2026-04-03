package com.pfip.transactionservice.controller;

import com.pfip.common.dto.ApiResponse;
import com.pfip.transactionservice.dto.TransactionDtos;
import com.pfip.transactionservice.entity.Transaction;
import com.pfip.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * All endpoints require authentication.
 * The caller's userId is read from the X-User-Id header forwarded by the API Gateway,
 * so users can only ever access their own data.
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final TransactionService transactionService;

    /** POST /api/transactions — create a new transaction */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> create(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody TransactionDtos.CreateTransactionRequest request) {

        TransactionDtos.TransactionResponse response = transactionService.create(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created", response));
    }

    /** GET /api/transactions — list transactions for the current user (paged) */
    @GetMapping
    public ResponseEntity<ApiResponse<TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse>>>
    getAll(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        var result = transactionService.getAllForUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** GET /api/transactions/{id} — get a single transaction */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> getById(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id) {

        return ResponseEntity.ok(ApiResponse.success(transactionService.getById(userId, id)));
    }

    /** GET /api/transactions/type/{type} — filter by CREDIT or DEBIT */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse>>>
    getByType(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Transaction.TransactionType type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByType(userId, type, pageable)));
    }

    /** GET /api/transactions/range?from=...&to=... — filter by date range */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse>>>
    getByDateRange(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByDateRange(userId, from, to, pageable)));
    }

    /** GET /api/transactions/summary — net balance and totals */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionSummary>> getSummary(
            @RequestHeader(USER_ID_HEADER) Long userId) {

        return ResponseEntity.ok(ApiResponse.success(transactionService.getSummary(userId)));
    }

    /** PATCH /api/transactions/{id}/status — update transaction status */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> updateStatus(
            @RequestHeader(USER_ID_HEADER) Long userId,
            @PathVariable Long id,
            @RequestParam Transaction.TransactionStatus status) {

        return ResponseEntity.ok(ApiResponse.success(
                "Status updated",
                transactionService.updateStatus(userId, id, status)));
    }
}