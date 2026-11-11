package com.ledgerline.controller;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.AccountStatusUpdateRequest;
import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.enums.Role;
import com.ledgerline.service.AccountService;
import com.ledgerline.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AccountDto account = accountService.createAccount(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", account));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto>>> getMyAccounts() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<AccountDto> accounts = accountService.getUserAccounts(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/recipients")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getTransferRecipients() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        List<AccountDto> recipients = accountService.getTransferRecipients(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(recipients));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<ApiResponse<AccountDto>> getAccountById(@PathVariable Long accountId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Role currentUserRole = SecurityUtils.getCurrentUserRole();
        AccountDto account = accountService.getAccountById(accountId, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @GetMapping("/{accountId}/balance")
    public ResponseEntity<ApiResponse<BigDecimal>> getAccountBalance(@PathVariable Long accountId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Role currentUserRole = SecurityUtils.getCurrentUserRole();
        BigDecimal balance = accountService.getAccountBalance(accountId, currentUserId, currentUserRole);
        return ResponseEntity.ok(ApiResponse.success(balance));
    }

    @PatchMapping("/{accountId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountStatusUpdateRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String adminUsername = SecurityUtils.getCurrentUser().getUsername();
        AccountDto updated = accountService.updateAccountStatus(accountId, request, adminId, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Account status updated to " + updated.getStatus(), updated));
    }
}
