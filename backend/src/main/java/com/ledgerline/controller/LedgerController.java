package com.ledgerline.controller;

import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.LedgerEntryDto;
import com.ledgerline.dto.LedgerVerificationResult;
import com.ledgerline.service.LedgerService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<Page<LedgerEntryDto>>> getAllEntries(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<LedgerEntryDto> entries = ledgerService.getAllLedgerEntries(pageable);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/transactions/{transactionId}")
    public ResponseEntity<ApiResponse<List<LedgerEntryDto>>> getEntriesForTransaction(@PathVariable Long transactionId) {
        List<LedgerEntryDto> entries = ledgerService.getEntriesByTransactionId(transactionId);
        return ResponseEntity.ok(ApiResponse.success(entries));
    }

    @GetMapping("/verify")
    public ResponseEntity<ApiResponse<LedgerVerificationResult>> verifyLedger() {
        LedgerVerificationResult result = ledgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(ApiResponse.success(
                result.isBalanced() ? "Ledger is balanced and mathematically verified." : "Ledger imbalance detected!",
                result
        ));
    }
}

