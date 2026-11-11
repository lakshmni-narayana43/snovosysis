package com.ledgerline.service;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.AccountStatusUpdateRequest;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.AuditAction;
import com.ledgerline.enums.AuditStatus;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.ForbiddenException;
import com.ledgerline.exception.InvalidAccountStateException;
import com.ledgerline.exception.ResourceNotFoundException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final SecureRandom random = new SecureRandom();

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AuditService auditService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public AccountDto createAccount(Long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String accountNumber = generateUniqueAccountNumber();
        String currency = request.getCurrency() != null ? request.getCurrency().toUpperCase() : "INR";

        Account account = new Account(
                accountNumber,
                user,
                request.getAccountType() != null ? request.getAccountType() : AccountType.SAVINGS,
                currency,
                BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
        );

        Account saved = accountRepository.save(account);

        auditService.recordAudit(
                user.getId(),
                user.getUsername(),
                AuditAction.ACCOUNT_CREATE,
                "ACCOUNT",
                saved.getAccountNumber(),
                AuditStatus.SUCCESS,
                null,
                null,
                "Account created: type=" + saved.getAccountType() + ", currency=" + saved.getCurrency()
        );

        return new AccountDto(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountById(Long accountId, Long currentUserId, Role currentUserRole) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        verifyOwnershipOrStaff(account, currentUserId, currentUserRole);
        return new AccountDto(account);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByNumber(String accountNumber, Long currentUserId, Role currentUserRole) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber));

        verifyOwnershipOrStaff(account, currentUserId, currentUserRole);
        return new AccountDto(account);
    }

    @Transactional(readOnly = true)
    public BigDecimal getAccountBalance(Long accountId, Long currentUserId, Role currentUserRole) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        verifyOwnershipOrStaff(account, currentUserId, currentUserRole);
        return account.getBalance();
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccountsForAdmin() {
        return accountRepository.findAll().stream()
                .map(AccountDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getTransferRecipients(Long currentUserId) {
        return accountRepository.findAll().stream()
                .filter(account -> account.getUser() != null)
                .filter(account -> !account.getUser().getId().equals(currentUserId))
                .filter(account -> account.getStatus() == AccountStatus.ACTIVE)
                .map(AccountDto::new)
                .toList();
    }

    @Transactional
    public AccountDto updateAccountStatus(Long accountId, AccountStatusUpdateRequest request,
                                         Long adminUserId, String adminUsername) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));

        if (account.getAccountType() == AccountType.SYSTEM_CLEARING) {
            throw new InvalidAccountStateException("System clearing accounts cannot have their status altered.");
        }

        AccountStatus oldStatus = account.getStatus();
        AccountStatus newStatus = request.getStatus();

        if (oldStatus == AccountStatus.CLOSED && newStatus != AccountStatus.CLOSED) {
            throw new InvalidAccountStateException("Closed accounts cannot be reopened.");
        }

        account.setStatus(newStatus);
        Account updated = accountRepository.save(account);

        AuditAction action = switch (newStatus) {
            case FROZEN -> AuditAction.ACCOUNT_FREEZE;
            case ACTIVE -> AuditAction.ACCOUNT_UNFREEZE;
            case CLOSED -> AuditAction.ACCOUNT_CLOSE;
        };

        auditService.recordAudit(
                adminUserId,
                adminUsername,
                action,
                "ACCOUNT",
                updated.getAccountNumber(),
                AuditStatus.SUCCESS,
                null,
                null,
                "Status changed from " + oldStatus + " to " + newStatus + ". Reason: " + request.getReason()
        );

        return new AccountDto(updated);
    }

    public void verifyOwnershipOrStaff(Account account, Long currentUserId, Role currentUserRole) {
        if (currentUserRole == Role.ROLE_ADMIN || currentUserRole == Role.ROLE_AUDITOR) {
            return;
        }

        if (account.getUser() == null || !account.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("Access denied: You do not own account " + account.getAccountNumber());
        }
    }

    private String generateUniqueAccountNumber() {
        String prefix = "LL";
        while (true) {
            long number = 1000000000L + (long) (random.nextDouble() * 9000000000L);
            String candidate = prefix + number;
            if (!accountRepository.existsByAccountNumber(candidate)) {
                return candidate;
            }
        }
    }
}
