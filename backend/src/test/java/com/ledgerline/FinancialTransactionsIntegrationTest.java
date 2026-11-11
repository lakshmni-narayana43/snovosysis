package com.ledgerline;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.LedgerVerificationResult;
import com.ledgerline.dto.TransactionDto;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.InsufficientBalanceException;
import com.ledgerline.exception.InvalidAccountStateException;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AccountService;
import com.ledgerline.service.LedgerService;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class FinancialTransactionsIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser1;
    private User testUser2;
    private AccountDto accountA;
    private AccountDto accountB;

    @BeforeEach
    void setupTestData() {
        String u1 = "testuser1_" + UUID.randomUUID().toString().substring(0, 8);
        String u2 = "testuser2_" + UUID.randomUUID().toString().substring(0, 8);

        testUser1 = userRepository.save(new User(u1, u1 + "@test.com", "pass", "Test User 1", Role.ROLE_ACCOUNT_HOLDER));
        testUser2 = userRepository.save(new User(u2, u2 + "@test.com", "pass", "Test User 2", Role.ROLE_ACCOUNT_HOLDER));

        accountA = accountService.createAccount(testUser1.getId(), new CreateAccountRequest(AccountType.SAVINGS, "INR", BigDecimal.ZERO));
        accountB = accountService.createAccount(testUser2.getId(), new CreateAccountRequest(AccountType.CHECKING, "INR", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Deposit creates balanced ledger entries and updates account balance")
    void testDepositLifecycle() {
        BigDecimal depositAmount = new BigDecimal("2500.0000");
        String idempotencyKey = "DEP-" + UUID.randomUUID();

        TransactionDto txn = transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), depositAmount, "INR", "Test deposit"),
                testUser1.getId(),
                testUser1.getRole(),
                idempotencyKey
        );

        assertNotNull(txn);
        assertEquals(depositAmount, txn.getAmount());
        assertEquals(2, txn.getLedgerEntries().size());

        BigDecimal updatedBalance = accountService.getAccountBalance(accountA.getId(), testUser1.getId(), testUser1.getRole());
        assertEquals(depositAmount, updatedBalance);
    }

    @Test
    @DisplayName("Withdrawal decreases balance and prevents overdrawing")
    void testWithdrawalAndOverdrawPrevention() {
        // First deposit ₹1000
        transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), new BigDecimal("1000.0000"), "INR", "Pre-fund"),
                testUser1.getId(),
                testUser1.getRole(),
                "FUND-" + UUID.randomUUID()
        );

        // Withdraw ₹400
        TransactionDto txn = transactionService.withdraw(
                new WithdrawRequest(accountA.getAccountNumber(), new BigDecimal("400.0000"), "INR", "Withdraw ₹400"),
                testUser1.getId(),
                testUser1.getRole(),
                "WITH-" + UUID.randomUUID()
        );

        assertEquals(new BigDecimal("400.0000"), txn.getAmount());
        BigDecimal balance = accountService.getAccountBalance(accountA.getId(), testUser1.getId(), testUser1.getRole());
        assertEquals(new BigDecimal("600.0000"), balance);

        // Attempt to withdraw ₹700 (which exceeds available ₹600)
        assertThrows(InsufficientBalanceException.class, () ->
                transactionService.withdraw(
                        new WithdrawRequest(accountA.getAccountNumber(), new BigDecimal("700.0000"), "INR", "Overdraw attempt"),
                        testUser1.getId(),
                        testUser1.getRole(),
                        "OVER-" + UUID.randomUUID()
                ));

        // Verify balance remained unchanged at ₹600
        BigDecimal postFailBalance = accountService.getAccountBalance(accountA.getId(), testUser1.getId(), testUser1.getRole());
        assertEquals(new BigDecimal("600.0000"), postFailBalance);
    }

    @Test
    @DisplayName("Scenario 4: Transfer ₹500 from Account A to Account B — Money is conserved, ledger balances")
    void testTransferMoneyConservation() {
        // Pre-fund Account A with ₹1000 and Account B with ₹200
        transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), new BigDecimal("1000.0000"), "INR", "Fund A"),
                testUser1.getId(),
                testUser1.getRole(),
                "FUND-A-" + UUID.randomUUID()
        );
        transactionService.deposit(
                new DepositRequest(accountB.getAccountNumber(), new BigDecimal("200.0000"), "INR", "Fund B"),
                testUser2.getId(),
                testUser2.getRole(),
                "FUND-B-" + UUID.randomUUID()
        );

        BigDecimal transferAmount = new BigDecimal("500.0000");

        // Execute transfer of ₹500
        TransactionDto txn = transactionService.transfer(
                new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), transferAmount, "INR", "Peer transfer"),
                testUser1.getId(),
                testUser1.getRole(),
                "XFER-" + UUID.randomUUID()
        );

        assertNotNull(txn);
        assertEquals(transferAmount, txn.getAmount());
        assertEquals(2, txn.getLedgerEntries().size());

        // Account A should be 1000 - 500 = 500
        BigDecimal balanceA = accountService.getAccountBalance(accountA.getId(), testUser1.getId(), testUser1.getRole());
        assertEquals(new BigDecimal("500.0000"), balanceA);

        // Account B should be 200 + 500 = 700
        BigDecimal balanceB = accountService.getAccountBalance(accountB.getId(), testUser2.getId(), testUser2.getRole());
        assertEquals(new BigDecimal("700.0000"), balanceB);

        // Total money conserved: 500 + 700 = 1200 (original 1000 + 200)
        assertEquals(new BigDecimal("1200.0000"), balanceA.add(balanceB));

        // Verify double-entry trial balance
        LedgerVerificationResult verification = ledgerService.verifyLedgerIntegrity();
        assertTrue(verification.isBalanced(), "Ledger must be globally balanced");
        assertEquals(0, verification.getImbalance().compareTo(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Scenario 6: Transfer from frozen account is rejected")
    void testTransferFromFrozenAccountIsRejected() {
        // Fund Account A
        transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), new BigDecimal("1000.0000"), "INR", "Fund A"),
                testUser1.getId(),
                testUser1.getRole(),
                "FUND-" + UUID.randomUUID()
        );

        // Freeze Account A
        Account acc = accountRepository.findById(accountA.getId()).orElseThrow();
        acc.setStatus(AccountStatus.FROZEN);
        accountRepository.save(acc);

        // Attempt transfer from frozen account
        assertThrows(InvalidAccountStateException.class, () ->
                transactionService.transfer(
                        new TransferRequest(accountA.getAccountNumber(), accountB.getAccountNumber(), new BigDecimal("100.0000"), "INR", "Transfer"),
                        testUser1.getId(),
                        testUser1.getRole(),
                        "XFER-FROZEN-" + UUID.randomUUID()
                ));
    }
}

