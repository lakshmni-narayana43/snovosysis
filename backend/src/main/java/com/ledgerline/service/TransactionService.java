package com.ledgerline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionDto;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.IdempotencyRecord;
import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.entity.Transaction;
import com.ledgerline.entity.User;
import com.ledgerline.enums.*;
import com.ledgerline.exception.*;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final AccountService accountService;
    private final ObjectMapper objectMapper;
    private final jakarta.persistence.EntityManager entityManager;

    @Value("${ledgerline.financial.system-clearing-account:ACC-SYSTEM-CLEARING}")
    private String systemClearingAccountNumber;

    public TransactionService(TransactionRepository transactionRepository,
                              AccountRepository accountRepository,
                              UserRepository userRepository,
                              LedgerService ledgerService,
                              IdempotencyService idempotencyService,
                              AuditService auditService,
                              AccountService accountService,
                              ObjectMapper objectMapper,
                              jakarta.persistence.EntityManager entityManager) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.accountService = accountService;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    // =========================================================================
    // 1. DEPOSIT
    // =========================================================================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionDto deposit(DepositRequest request, Long authenticatedUserId,
                                  Role authenticatedRole, String idempotencyKey) {
        // Idempotency check
        String requestHash = idempotencyService.computePayloadHash("/api/transactions/deposit", request);
        Optional<IdempotencyRecord> existing = idempotencyService.checkOrCreate(idempotencyKey, authenticatedUserId, requestHash);
        if (existing.isPresent() && existing.get().getStatus() == IdempotencyStatus.COMPLETED) {
            try {
                return objectMapper.readValue(existing.get().getResponseBody(), TransactionDto.class);
            } catch (Exception e) {
                log.error("Failed to parse cached idempotent deposit response", e);
            }
        }

        try {
            // Validate amount
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Deposit amount must be strictly greater than zero");
            }
            BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_UP);

            // Fetch user
            User initiator = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedUserId));

            // Acquire pessimistic write lock directly and refresh state from database
            Account lockedAccount = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));
            entityManager.refresh(lockedAccount, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

            // Verify ownership
            accountService.verifyOwnershipOrStaff(lockedAccount, authenticatedUserId, authenticatedRole);

            if (lockedAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidAccountStateException("Cannot deposit to account with status: " + lockedAccount.getStatus());
            }

            // Find system clearing account
            Account clearingAccount = getOrCreateClearingAccount();
            Account lockedClearingAccount = accountRepository.findByIdForUpdate(clearingAccount.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Failed to acquire lock on clearing account"));

            // Create Transaction record
            String txnRef = generateTransactionReference();
            Transaction transaction = new Transaction(
                    txnRef,
                    TransactionType.DEPOSIT,
                    TransactionStatus.COMPLETED,
                    amount,
                    lockedAccount.getCurrency(),
                    request.getDescription() != null ? request.getDescription() : "Account deposit",
                    lockedClearingAccount,
                    lockedAccount,
                    initiator,
                    idempotencyKey
            );
            transaction.setCompletedAt(OffsetDateTime.now());
            Transaction savedTxn = transactionRepository.save(transaction);

            // Double-entry ledger:
            // 1. Debit Clearing/Cash Account (Asset +)
            // 2. Credit Customer Account (Liability + / Customer balance +)
            LedgerEntry debitEntry = ledgerService.recordEntry(savedTxn, lockedClearingAccount, EntryType.DEBIT, amount);
            LedgerEntry creditEntry = ledgerService.recordEntry(savedTxn, lockedAccount, EntryType.CREDIT, amount);

            savedTxn.addLedgerEntry(debitEntry);
            savedTxn.addLedgerEntry(creditEntry);

            // Validate that Debits == Credits == Amount
            ledgerService.validateTransactionBalance(savedTxn, List.of(debitEntry, creditEntry));

            // Atomic balance updates
            lockedAccount.setBalance(lockedAccount.getBalance().add(amount));
            lockedClearingAccount.setBalance(lockedClearingAccount.getBalance().add(amount));
            accountRepository.save(lockedAccount);
            accountRepository.save(lockedClearingAccount);

            TransactionDto result = new TransactionDto(savedTxn);

            // Record success in idempotency & audit
            idempotencyService.recordSuccess(idempotencyKey, 200, result, txnRef);
            auditService.recordAudit(
                    initiator.getId(),
                    initiator.getUsername(),
                    AuditAction.DEPOSIT,
                    "TRANSACTION",
                    txnRef,
                    AuditStatus.SUCCESS,
                    null,
                    null,
                    "Deposit of " + amount + " " + lockedAccount.getCurrency() + " to " + lockedAccount.getAccountNumber()
            );

            return result;
        } catch (Exception ex) {
            idempotencyService.recordFailure(idempotencyKey, 400, ex.getMessage());
            auditService.recordAudit(
                    authenticatedUserId,
                    "User#" + authenticatedUserId,
                    AuditAction.TRANSACTION_FAILED,
                    "TRANSACTION",
                    null,
                    AuditStatus.FAILURE,
                    null,
                    ex.getMessage(),
                    "Deposit failed: " + ex.getMessage()
            );
            throw ex;
        }
    }

    // =========================================================================
    // 2. WITHDRAWAL
    // =========================================================================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionDto withdraw(WithdrawRequest request, Long authenticatedUserId,
                                   Role authenticatedRole, String idempotencyKey) {
        String requestHash = idempotencyService.computePayloadHash("/api/transactions/withdraw", request);
        Optional<IdempotencyRecord> existing = idempotencyService.checkOrCreate(idempotencyKey, authenticatedUserId, requestHash);
        if (existing.isPresent() && existing.get().getStatus() == IdempotencyStatus.COMPLETED) {
            try {
                return objectMapper.readValue(existing.get().getResponseBody(), TransactionDto.class);
            } catch (Exception e) {
                log.error("Failed to parse cached idempotent withdraw response", e);
            }
        }

        try {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Withdrawal amount must be strictly greater than zero");
            }
            BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_UP);

            User initiator = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedUserId));

            // Acquire pessimistic write lock directly and refresh state from database
            Account lockedAccount = accountRepository.findByAccountNumberForUpdate(request.getAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + request.getAccountNumber()));
            entityManager.refresh(lockedAccount, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

            accountService.verifyOwnershipOrStaff(lockedAccount, authenticatedUserId, authenticatedRole);

            if (lockedAccount.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidAccountStateException("Cannot withdraw from account with status: " + lockedAccount.getStatus());
            }

            // Check available balance after lock acquisition
            if (lockedAccount.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(String.format(
                        "Insufficient balance in account %s. Available: %s, Requested: %s",
                        lockedAccount.getAccountNumber(), lockedAccount.getBalance(), amount));
            }

            // Lock clearing account
            Account clearingAccount = getOrCreateClearingAccount();
            Account lockedClearingAccount = accountRepository.findByIdForUpdate(clearingAccount.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Failed to acquire lock on clearing account"));

            String txnRef = generateTransactionReference();
            Transaction transaction = new Transaction(
                    txnRef,
                    TransactionType.WITHDRAWAL,
                    TransactionStatus.COMPLETED,
                    amount,
                    lockedAccount.getCurrency(),
                    request.getDescription() != null ? request.getDescription() : "Account withdrawal",
                    lockedAccount,
                    lockedClearingAccount,
                    initiator,
                    idempotencyKey
            );
            transaction.setCompletedAt(OffsetDateTime.now());
            Transaction savedTxn = transactionRepository.save(transaction);

            // Double-entry ledger:
            // 1. Debit Customer Account (Liability - / Customer balance -)
            // 2. Credit Clearing Account (Asset - / Cash dispensed)
            LedgerEntry debitEntry = ledgerService.recordEntry(savedTxn, lockedAccount, EntryType.DEBIT, amount);
            LedgerEntry creditEntry = ledgerService.recordEntry(savedTxn, lockedClearingAccount, EntryType.CREDIT, amount);

            savedTxn.addLedgerEntry(debitEntry);
            savedTxn.addLedgerEntry(creditEntry);

            ledgerService.validateTransactionBalance(savedTxn, List.of(debitEntry, creditEntry));

            // Atomic balance updates
            lockedAccount.setBalance(lockedAccount.getBalance().subtract(amount));
            lockedClearingAccount.setBalance(lockedClearingAccount.getBalance().subtract(amount));
            accountRepository.save(lockedAccount);
            accountRepository.save(lockedClearingAccount);

            TransactionDto result = new TransactionDto(savedTxn);

            idempotencyService.recordSuccess(idempotencyKey, 200, result, txnRef);
            auditService.recordAudit(
                    initiator.getId(),
                    initiator.getUsername(),
                    AuditAction.WITHDRAWAL,
                    "TRANSACTION",
                    txnRef,
                    AuditStatus.SUCCESS,
                    null,
                    null,
                    "Withdrawal of " + amount + " " + lockedAccount.getCurrency() + " from " + lockedAccount.getAccountNumber()
            );

            return result;
        } catch (Exception ex) {
            idempotencyService.recordFailure(idempotencyKey, 400, ex.getMessage());
            auditService.recordAudit(
                    authenticatedUserId,
                    "User#" + authenticatedUserId,
                    AuditAction.TRANSACTION_FAILED,
                    "TRANSACTION",
                    null,
                    AuditStatus.FAILURE,
                    null,
                    ex.getMessage(),
                    "Withdrawal failed: " + ex.getMessage()
            );
            throw ex;
        }
    }

    // =========================================================================
    // 3. TRANSFER
    // =========================================================================
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionDto transfer(TransferRequest request, Long authenticatedUserId,
                                   Role authenticatedRole, String idempotencyKey) {
        String requestHash = idempotencyService.computePayloadHash("/api/transactions/transfer", request);
        Optional<IdempotencyRecord> existing = idempotencyService.checkOrCreate(idempotencyKey, authenticatedUserId, requestHash);
        if (existing.isPresent() && existing.get().getStatus() == IdempotencyStatus.COMPLETED) {
            try {
                return objectMapper.readValue(existing.get().getResponseBody(), TransactionDto.class);
            } catch (Exception e) {
                log.error("Failed to parse cached idempotent transfer response", e);
            }
        }

        try {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidTransactionException("Transfer amount must be strictly greater than zero");
            }
            BigDecimal amount = request.getAmount().setScale(4, RoundingMode.HALF_UP);

            if (request.getSourceAccountNumber().trim().equalsIgnoreCase(request.getDestinationAccountNumber().trim())) {
                throw new InvalidTransactionException("Source and destination accounts cannot be identical");
            }

            User initiator = userRepository.findById(authenticatedUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedUserId));

            Account sourceAccount = accountRepository.findByAccountNumber(request.getSourceAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Source account not found: " + request.getSourceAccountNumber()));

            Account destAccount = accountRepository.findByAccountNumber(request.getDestinationAccountNumber())
                    .orElseThrow(() -> new ResourceNotFoundException("Destination account not found: " + request.getDestinationAccountNumber()));

            // Verify sender ownership
            accountService.verifyOwnershipOrStaff(sourceAccount, authenticatedUserId, authenticatedRole);

            // DEADLOCK PREVENTION: Deterministic Lock Acquisition Order
            // Always lock the account with the smaller numerical ID first!
            Account lockedSource;
            Account lockedDest;

            if (sourceAccount.getId() < destAccount.getId()) {
                lockedSource = accountRepository.findByIdForUpdate(sourceAccount.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Lock failed on source account"));
                entityManager.refresh(lockedSource, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

                lockedDest = accountRepository.findByIdForUpdate(destAccount.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Lock failed on destination account"));
                entityManager.refresh(lockedDest, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            } else {
                lockedDest = accountRepository.findByIdForUpdate(destAccount.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Lock failed on destination account"));
                entityManager.refresh(lockedDest, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);

                lockedSource = accountRepository.findByIdForUpdate(sourceAccount.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Lock failed on source account"));
                entityManager.refresh(lockedSource, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
            }

            // Post-lock status verification
            if (lockedSource.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidAccountStateException("Source account is not active (status: " + lockedSource.getStatus() + ")");
            }
            if (lockedDest.getStatus() != AccountStatus.ACTIVE) {
                throw new InvalidAccountStateException("Destination account is not active (status: " + lockedDest.getStatus() + ")");
            }

            // Post-lock balance verification
            if (lockedSource.getBalance().compareTo(amount) < 0) {
                throw new InsufficientBalanceException(String.format(
                        "Insufficient balance in source account %s. Available: %s, Required: %s",
                        lockedSource.getAccountNumber(), lockedSource.getBalance(), amount));
            }

            // Create Transaction Record
            String txnRef = generateTransactionReference();
            Transaction transaction = new Transaction(
                    txnRef,
                    TransactionType.TRANSFER,
                    TransactionStatus.COMPLETED,
                    amount,
                    lockedSource.getCurrency(),
                    request.getDescription() != null ? request.getDescription() : "Peer transfer",
                    lockedSource,
                    lockedDest,
                    initiator,
                    idempotencyKey
            );
            transaction.setCompletedAt(OffsetDateTime.now());
            Transaction savedTxn = transactionRepository.save(transaction);

            // Double-entry ledger:
            // 1. Debit Source Account (Liability - / Balance -)
            // 2. Credit Destination Account (Liability + / Balance +)
            // INVARIANT: Sender Debit == Receiver Credit == Transfer Amount
            LedgerEntry debitEntry = ledgerService.recordEntry(savedTxn, lockedSource, EntryType.DEBIT, amount);
            LedgerEntry creditEntry = ledgerService.recordEntry(savedTxn, lockedDest, EntryType.CREDIT, amount);

            savedTxn.addLedgerEntry(debitEntry);
            savedTxn.addLedgerEntry(creditEntry);

            ledgerService.validateTransactionBalance(savedTxn, List.of(debitEntry, creditEntry));

            // Atomic balance updates
            lockedSource.setBalance(lockedSource.getBalance().subtract(amount));
            lockedDest.setBalance(lockedDest.getBalance().add(amount));
            accountRepository.save(lockedSource);
            accountRepository.save(lockedDest);

            TransactionDto result = new TransactionDto(savedTxn);

            idempotencyService.recordSuccess(idempotencyKey, 200, result, txnRef);
            auditService.recordAudit(
                    initiator.getId(),
                    initiator.getUsername(),
                    AuditAction.TRANSFER,
                    "TRANSACTION",
                    txnRef,
                    AuditStatus.SUCCESS,
                    null,
                    null,
                    String.format("Transfer of %s %s from %s to %s",
                            amount, lockedSource.getCurrency(), lockedSource.getAccountNumber(), lockedDest.getAccountNumber())
            );

            return result;
        } catch (Exception ex) {
            idempotencyService.recordFailure(idempotencyKey, 400, ex.getMessage());
            auditService.recordAudit(
                    authenticatedUserId,
                    "User#" + authenticatedUserId,
                    AuditAction.TRANSACTION_FAILED,
                    "TRANSACTION",
                    null,
                    AuditStatus.FAILURE,
                    null,
                    ex.getMessage(),
                    "Transfer failed: " + ex.getMessage()
            );
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public TransactionDto getTransactionByReference(String reference, Long authenticatedUserId, Role authenticatedRole) {
        Transaction transaction = transactionRepository.findByTransactionReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + reference));

        verifyTransactionAccess(transaction, authenticatedUserId, authenticatedRole);
        return new TransactionDto(transaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getUserTransactions(Long userId, Pageable pageable) {
        return transactionRepository.findByUserId(userId, pageable)
                .map(TransactionDto::new);
    }

    @Transactional(readOnly = true)
    public Page<TransactionDto> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(TransactionDto::new);
    }

    private void verifyTransactionAccess(Transaction transaction, Long currentUserId, Role currentUserRole) {
        if (currentUserRole == Role.ROLE_ADMIN || currentUserRole == Role.ROLE_AUDITOR) {
            return;
        }

        boolean isInitiator = transaction.getInitiatedBy().getId().equals(currentUserId);
        boolean isSourceOwner = transaction.getSourceAccount() != null
                && transaction.getSourceAccount().getUser() != null
                && transaction.getSourceAccount().getUser().getId().equals(currentUserId);
        boolean isDestOwner = transaction.getDestinationAccount() != null
                && transaction.getDestinationAccount().getUser() != null
                && transaction.getDestinationAccount().getUser().getId().equals(currentUserId);

        if (!isInitiator && !isSourceOwner && !isDestOwner) {
            throw new ForbiddenException("Access denied: You do not have permission to view transaction "
                    + transaction.getTransactionReference());
        }
    }

    private Account getOrCreateClearingAccount() {
        return accountRepository.findByAccountNumber(systemClearingAccountNumber)
                .orElseGet(() -> {
                    Account clearing = new Account(
                            systemClearingAccountNumber,
                            null,
                            AccountType.SYSTEM_CLEARING,
                            "INR",
                            BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                    );
                    return accountRepository.save(clearing);
                });
    }

    private String generateTransactionReference() {
        return "TXN-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
