package com.ledgerline.controller;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.AccountStatusUpdateRequest;
import com.ledgerline.dto.ApiResponse;
import com.ledgerline.dto.TransactionDto;
import com.ledgerline.dto.UserDto;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AccountService;
import com.ledgerline.service.TransactionService;
import com.ledgerline.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public AdminController(UserRepository userRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AccountService accountService,
                           TransactionService transactionService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(UserDto::new)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/accounts")
    public ResponseEntity<ApiResponse<List<AccountDto>>> getAllAccounts() {
        List<AccountDto> accounts = accountService.getAllAccountsForAdmin();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionDto>>> getAllTransactions(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<TransactionDto> transactions = transactionService.getAllTransactions(pageable);
        return ResponseEntity.ok(ApiResponse.success(transactions));
    }

    @PatchMapping("/accounts/{accountId}/status")
    public ResponseEntity<ApiResponse<AccountDto>> updateAccountStatus(
            @PathVariable Long accountId,
            @Valid @RequestBody AccountStatusUpdateRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        String adminUsername = SecurityUtils.getCurrentUser().getUsername();
        AccountDto updated = accountService.updateAccountStatus(accountId, request, adminId, adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Account status updated to " + updated.getStatus(), updated));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalAccounts", accountRepository.count());
        stats.put("totalTransactions", transactionRepository.count());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}

