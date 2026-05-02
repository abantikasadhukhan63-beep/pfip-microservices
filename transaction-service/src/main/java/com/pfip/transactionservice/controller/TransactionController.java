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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * All endpoints require authentication via JwtAuthenticationFilter.
 *
 * userId is read exclusively from the Spring Security context — populated
 * by JwtAuthenticationFilter after auth-service validates the token.
 *
 * The X-User-Id header injected by the gateway is intentionally ignored
 * here. Using the security context means even a direct request to port 8082
 * with a forged X-User-Id header cannot impersonate another user — the
 * userId always comes from auth-service's verified response.
 */
@Slf4j
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Reads the verified userId stored by JwtAuthenticationFilter.
     * The filter calls auth-service, gets back { userId, username, role },
     * and stores userId in authentication.details.
     * This method retrieves it — no header parsing, no JWT re-validation.
     */
    private Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof Long userId) {
            return userId;
        }
        throw new IllegalStateException(
                "User ID not found in security context. " +
                        "Ensure JwtAuthenticationFilter ran successfully.");
    }

    // ── Endpoints ─────────────────────────────────────────────────────────────

    /**
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> create(
            @Valid @RequestBody TransactionDtos.CreateTransactionRequest request) {

        Long userId = getCurrentUserId();
        log.info("Creating transaction for userId: {}, type: {}, amount: {}",
                userId, request.getType(), request.getAmount());

        TransactionDtos.TransactionResponse response =
                transactionService.create(userId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transaction created", response));
    }

    /**
     * GET /api/transactions
     */
    @GetMapping
    public ResponseEntity<ApiResponse<TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse>>>
    getAll(
            @RequestParam(defaultValue = "0")          int page,
            @RequestParam(defaultValue = "20")         int size,
            @RequestParam(defaultValue = "createdAt")  String sortBy,
            @RequestParam(defaultValue = "desc")       String direction) {

        Long userId = getCurrentUserId();

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        var result = transactionService.getAllForUser(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> getById(
            @PathVariable Long id) {

        Long userId = getCurrentUserId();
        log.debug("Fetching transaction id: {} for userId: {}", id, userId);

        return ResponseEntity.ok(
                ApiResponse.success(transactionService.getById(userId, id)));
    }

    /**
     * GET /api/transactions/type/{type}
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse>>>
    getByType(
            @PathVariable Transaction.TransactionType type,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByType(userId, type, pageable)));
    }

    /**
     * GET /api/transactions/range?from=...&to=...
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<TransactionDtos.PagedResponse<TransactionDtos.TransactionResponse>>>
    getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Long userId = getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size,
                Sort.by("createdAt").descending());

        return ResponseEntity.ok(ApiResponse.success(
                transactionService.getByDateRange(userId, from, to, pageable)));
    }

    /**
     * GET /api/transactions/summary
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionSummary>> getSummary() {

        Long userId = getCurrentUserId();
        log.debug("Fetching summary for userId: {}", userId);

        return ResponseEntity.ok(
                ApiResponse.success(transactionService.getSummary(userId)));
    }

    /**
     * PATCH /api/transactions/{id}/status?status=COMPLETED
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TransactionDtos.TransactionResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam Transaction.TransactionStatus status) {

        Long userId = getCurrentUserId();
        log.info("Updating status of transaction id: {} to {} for userId: {}",
                id, status, userId);

        return ResponseEntity.ok(ApiResponse.success(
                "Status updated",
                transactionService.updateStatus(userId, id, status)));
    }
}