package com.ledgerline.controller;

import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionDto;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.enums.Role;
import com.ledgerline.service.TransactionService;
import com.ledgerline.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<TransactionDto>> deposit(
            @Valid @RequestBody DepositRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String key = resolveIdempotencyKey(idempotencyKey);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Role currentUserRole = SecurityUtils.getCurrentUserRole();

        TransactionDto result = transactionService.deposit(request, currentUserId, currentUserRole, key);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Idempotency-Key", key)
                .body(ApiResponse.success("Deposit processed successfully", result));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<TransactionDto>> withdraw(
            @Valid @RequestBody WithdrawRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String key = resolveIdempotencyKey(idempotencyKey);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Role currentUserRole = SecurityUtils.getCurrentUserRole();

        TransactionDto result = transactionService.withdraw(request, currentUserId, currentUserRole, key);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Idempotency-Key", key)
                .body(ApiResponse.success("Withdrawal processed successfully", result));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransactionDto>> transfer(
            @Valid @RequestBody TransferRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        String key = resolveIdempotencyKey(idempotencyKey);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Role currentUserRole = SecurityUtils.getCurrentUserRole();

        TransactionDto result = transactionService.transfer(request, currentUserId, currentUserRole, key);
        return ResponseEntity.status(HttpStatus.OK)
                .header("Idempotency-Key", key)
                .body(ApiResponse.success("Transfer completed successfully", result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getMyTransactions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<TransactionDto> transactions = transactionService.getUserTransactions(currentUserId, pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/{transactionRef}")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransactionByReference(@PathVariable String transactionRef) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Role currentUserRole = SecurityUtils.getCurrentUserRole();
        TransactionDto transaction = transactionService.getTransactionByReference(transactionRef, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    private String resolveIdempotencyKey(String headerKey) {
        if (headerKey != null && !headerKey.trim().isEmpty()) {
            return headerKey.trim();
        }
        return UUID.randomUUID().toString();
    }
}

