package com.ledgerline.service;

import com.ledgerline.dto.AccountReconciliationDto;
import com.ledgerline.dto.LedgerEntryDto;
import com.ledgerline.dto.LedgerVerificationResult;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.entity.Transaction;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.EntryType;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.exception.LedgerImbalanceException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.LedgerEntryRepository;
import com.ledgerline.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository,
                         TransactionRepository transactionRepository,
                         AccountRepository accountRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public LedgerEntry recordEntry(Transaction transaction, Account account, EntryType entryType, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Ledger entry amount must be strictly greater than zero");
        }

        BigDecimal scaledAmount = amount.setScale(4, RoundingMode.HALF_UP);
        LedgerEntry entry = new LedgerEntry(transaction, account, entryType, scaledAmount, transaction.getCurrency());
        return ledgerEntryRepository.save(entry);
    }

    public void validateTransactionBalance(Transaction transaction, List<LedgerEntry> entries) {
        if (entries == null || entries.size() < 2) {
            throw new LedgerImbalanceException(
                    "Transaction " + transaction.getTransactionReference() + " must have at least 2 ledger entries.");
        }

        BigDecimal totalDebits = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalCredits = BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);

        for (LedgerEntry entry : entries) {
            if (entry.getEntryType() == EntryType.DEBIT) {
                totalDebits = totalDebits.add(entry.getAmount());
            } else if (entry.getEntryType() == EntryType.CREDIT) {
                totalCredits = totalCredits.add(entry.getAmount());
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            String errorMsg = String.format(
                    "LEDGER IMBALANCE for TXN %s: Debits (%s) != Credits (%s)",
                    transaction.getTransactionReference(), totalDebits, totalCredits);
            log.error(errorMsg);
            throw new LedgerImbalanceException(errorMsg);
        }

        BigDecimal transactionAmount = transaction.getAmount().setScale(4, RoundingMode.HALF_UP);
        if (totalDebits.compareTo(transactionAmount) != 0) {
            String errorMsg = String.format(
                    "LEDGER AMOUNT MISMATCH for TXN %s: Expected %s but debits were %s",
                    transaction.getTransactionReference(), transactionAmount, totalDebits);
            log.error(errorMsg);
            throw new LedgerImbalanceException(errorMsg);
        }
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDto> getEntriesByTransactionId(Long transactionId) {
        return ledgerEntryRepository.findByTransactionId(transactionId).stream()
                .map(LedgerEntryDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> getEntriesByAccountId(Long accountId, Pageable pageable) {
        return ledgerEntryRepository.findByAccountId(accountId, pageable)
                .map(LedgerEntryDto::new);
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> getAllLedgerEntries(Pageable pageable) {
        return ledgerEntryRepository.findAll(pageable)
                .map(LedgerEntryDto::new);
    }

    @Transactional(readOnly = true)
    public LedgerVerificationResult verifyLedgerIntegrity() {
        LedgerVerificationResult result = new LedgerVerificationResult();

        long totalTxns = transactionRepository.count();
        long totalEntries = ledgerEntryRepository.count();
        result.setTotalTransactions(totalTxns);
        result.setTotalLedgerEntries(totalEntries);

        BigDecimal totalDebits = ledgerEntryRepository.sumTotalByEntryType(EntryType.DEBIT).setScale(4, RoundingMode.HALF_UP);
        BigDecimal totalCredits = ledgerEntryRepository.sumTotalByEntryType(EntryType.CREDIT).setScale(4, RoundingMode.HALF_UP);
        BigDecimal imbalance = totalDebits.subtract(totalCredits).abs().setScale(4, RoundingMode.HALF_UP);

        result.setTotalDebits(totalDebits);
        result.setTotalCredits(totalCredits);
        result.setImbalance(imbalance);

        boolean isGlobalBalanced = imbalance.compareTo(BigDecimal.ZERO) == 0;
        result.setBalanced(isGlobalBalanced);

        if (!isGlobalBalanced) {
            result.addIssue(String.format("GLOBAL TRIAL BALANCE ERROR: Total Debits (%s) != Total Credits (%s)",
                    totalDebits, totalCredits));
        }

        // Reconcile each individual account
        List<Account> accounts = accountRepository.findAll();
        List<AccountReconciliationDto> reconciliations = new ArrayList<>();

        for (Account account : accounts) {
            BigDecimal accDebits = ledgerEntryRepository.sumTotalByAccountAndEntryType(account.getId(), EntryType.DEBIT)
                    .setScale(4, RoundingMode.HALF_UP);
            BigDecimal accCredits = ledgerEntryRepository.sumTotalByAccountAndEntryType(account.getId(), EntryType.CREDIT)
                    .setScale(4, RoundingMode.HALF_UP);

            BigDecimal calculatedBalance;
            if (account.getAccountType() == AccountType.SYSTEM_CLEARING) {
                // For the bank/clearing asset account: Balance = Debits - Credits
                calculatedBalance = accDebits.subtract(accCredits).setScale(4, RoundingMode.HALF_UP);
            } else {
                // For customer deposit liability accounts: Balance = Credits - Debits
                calculatedBalance = accCredits.subtract(accDebits).setScale(4, RoundingMode.HALF_UP);
            }

            BigDecimal currentBalance = account.getBalance().setScale(4, RoundingMode.HALF_UP);
            boolean isConsistent = currentBalance.compareTo(calculatedBalance) == 0;

            if (!isConsistent) {
                result.setBalanced(false);
                result.addIssue(String.format(
                        "DISCREPANCY in Account %s: DB Balance = %s, Ledger Balance = %s (Credits=%s, Debits=%s)",
                        account.getAccountNumber(), currentBalance, calculatedBalance, accCredits, accDebits));
            }

            reconciliations.add(new AccountReconciliationDto(
                    account.getId(),
                    account.getAccountNumber(),
                    currentBalance,
                    calculatedBalance,
                    accDebits,
                    accCredits,
                    isConsistent
            ));
        }

        result.setAccountReconciliations(reconciliations);
        return result;
    }
}

