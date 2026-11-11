package com.ledgerline;

import com.ledgerline.entity.Account;
import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.entity.Transaction;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.EntryType;
import com.ledgerline.enums.Role;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import com.ledgerline.exception.LedgerImbalanceException;
import com.ledgerline.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LedgerMathUnitTest {

    private LedgerService ledgerService;

    @BeforeEach
    void setUp() {
        // Pure unit testing of mathematical and balance validation rules
        ledgerService = new LedgerService(null, null, null);
    }

    @Test
    @DisplayName("Debits and credits equal transaction amount: Validation passes")
    void testBalancedLedgerValidation() {
        BigDecimal amount = new BigDecimal("500.0000");
        User user = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        Account source = new Account("ACC-1", user, AccountType.SAVINGS, "INR", new BigDecimal("1000.0000"));
        Account dest = new Account("ACC-2", user, AccountType.CHECKING, "INR", new BigDecimal("200.0000"));

        Transaction txn = new Transaction("TXN-1", TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                amount, "INR", "Transfer", source, dest, user, "KEY-1");

        LedgerEntry debit = new LedgerEntry(txn, source, EntryType.DEBIT, amount, "INR");
        LedgerEntry credit = new LedgerEntry(txn, dest, EntryType.CREDIT, amount, "INR");

        assertDoesNotThrow(() -> ledgerService.validateTransactionBalance(txn, List.of(debit, credit)));
    }

    @Test
    @DisplayName("Imbalanced ledger (Debits != Credits) throws LedgerImbalanceException")
    void testImbalancedLedgerThrowsException() {
        BigDecimal amount = new BigDecimal("500.0000");
        User user = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        Account source = new Account("ACC-1", user, AccountType.SAVINGS, "INR", new BigDecimal("1000.0000"));
        Account dest = new Account("ACC-2", user, AccountType.CHECKING, "INR", new BigDecimal("200.0000"));

        Transaction txn = new Transaction("TXN-1", TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                amount, "INR", "Transfer", source, dest, user, "KEY-1");

        LedgerEntry debit = new LedgerEntry(txn, source, EntryType.DEBIT, amount, "INR");
        LedgerEntry wrongCredit = new LedgerEntry(txn, dest, EntryType.CREDIT, new BigDecimal("499.0000"), "INR");

        assertThrows(LedgerImbalanceException.class, () ->
                ledgerService.validateTransactionBalance(txn, List.of(debit, wrongCredit)));
    }

    @Test
    @DisplayName("Transaction with single entry throws LedgerImbalanceException")
    void testSingleEntryThrowsException() {
        BigDecimal amount = new BigDecimal("500.0000");
        User user = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        Account source = new Account("ACC-1", user, AccountType.SAVINGS, "INR", new BigDecimal("1000.0000"));

        Transaction txn = new Transaction("TXN-1", TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                amount, "INR", "Transfer", source, null, user, "KEY-1");

        LedgerEntry debit = new LedgerEntry(txn, source, EntryType.DEBIT, amount, "INR");

        assertThrows(LedgerImbalanceException.class, () ->
                ledgerService.validateTransactionBalance(txn, List.of(debit)));
    }
}
