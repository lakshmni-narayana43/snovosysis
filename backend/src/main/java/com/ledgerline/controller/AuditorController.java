package com.ledgerline.controller;

import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.LedgerEntryDto;
import com.ledgerline.dto.LedgerVerificationResult;
import com.ledgerline.dto.TransactionDto;
import com.ledgerline.service.LedgerService;
import com.ledgerline.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auditor")
@PreAuthorize("hasAnyRole('AUDITOR', 'ADMIN')")
public class AuditorController {

    private final TransactionService transactionService;
    private final LedgerService ledgerService;

    public AuditorController(TransactionService transactionService,
                             LedgerService ledgerService) {
        this.transactionService = transactionService;
        this.ledgerService = ledgerService;
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getTransactionsForAudit(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @GetMapping("/ledger")
    public ResponseEntity<ApiResponse<Page<LedgerEntryDto>>> getLedgerEntriesForAudit(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LedgerEntryDto> entries = ledgerService.getAllLedgerEntries(pageable);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<LedgerVerificationResult>> runIntegrityCheck() {
        LedgerVerificationResult result = ledgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(ApiResponse.success(
                result.isBalanced() ? "Double-entry ledger is mathematically verified." : "Audit warning: Ledger imbalance detected!",
                result
        ));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuditReport() {
        LedgerVerificationResult verification = ledgerService.verifyLedgerIntegrity();

        Map<String, Object> report = new HashMap<>();
        report.put("verificationSummary", verification);
        report.put("auditStatus", verification.isBalanced() ? "PASS" : "FAIL");
        report.put("totalIssuesDetected", verification.getIssues().size());
        report.put("issues", verification.getIssues());
        report.put("accountReconciliations", verification.getAccountReconciliations());

        return ResponseEntity.ok(ApiResponse.success("Comprehensive Audit Report", report));
    }
}

