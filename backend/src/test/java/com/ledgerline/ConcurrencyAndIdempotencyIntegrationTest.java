package com.ledgerline;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionDto;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.IdempotencyConflictException;
import com.ledgerline.exception.InsufficientBalanceException;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AccountService;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyAndIdempotencyIntegrationTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private UserRepository userRepository;

    private User userA;
    private User userB;
    private AccountDto accountA;
    private AccountDto accountB;

    @BeforeEach
    void setupTestData() {
        String u1 = "conc_user1_" + UUID.randomUUID().toString().substring(0, 8);
        String u2 = "conc_user2_" + UUID.randomUUID().toString().substring(0, 8);

        userA = userRepository.save(new User(u1, u1 + "@test.com", "pass", "User A", Role.ROLE_ACCOUNT_HOLDER));
        userB = userRepository.save(new User(u2, u2 + "@test.com", "pass", "User B", Role.ROLE_ACCOUNT_HOLDER));

        accountA = accountService.createAccount(userA.getId(), new CreateAccountRequest(AccountType.SAVINGS, "INR", BigDecimal.ZERO));
        accountB = accountService.createAccount(userB.getId(), new CreateAccountRequest(AccountType.CHECKING, "INR", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Scenario 1: Concurrency Protection — Account has ₹1,000. Two simultaneous withdrawals of ₹800. Exactly 1 succeeds, 1 fails, final balance is ₹200.")
    void testConcurrentWithdrawalsRaceCondition() throws Exception {
        // Deposit initial balance of ₹1,000
        transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), new BigDecimal("1000.0000"), "INR", "Initial ₹1000"),
                userA.getId(),
                userA.getRole(),
                "INIT-" + UUID.randomUUID()
        );

        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<Throwable> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threads; i++) {
            final String threadKey = "WITHDRAW-RACE-" + i + "-" + UUID.randomUUID();
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    transactionService.withdraw(
                            new WithdrawRequest(accountA.getAccountNumber(), new BigDecimal("800.0000"), "INR", "Simultaneous ₹800"),
                            userA.getId(),
                            userA.getRole(),
                            threadKey
                    );
                    successCount.incrementAndGet();
                } catch (InsufficientBalanceException e) {
                    failureCount.incrementAndGet();
                } catch (Throwable t) {
                    exceptions.add(t);
                }
            });
        }

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Release both threads at the exact same millisecond
        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);

        assertTrue(finished, "Executor threads should finish within timeout");
        assertTrue(exceptions.isEmpty(), "Unexpected exceptions occurred: " + exceptions);

        // Assert exactly one succeeded and one failed
        assertEquals(1, successCount.get(), "Exactly one withdrawal of ₹800 should succeed");
        assertEquals(1, failureCount.get(), "Exactly one withdrawal should fail due to insufficient balance");

        // Assert final balance is exactly ₹200.0000 (1000 - 800) and NEVER negative
        BigDecimal finalBalance = accountService.getAccountBalance(accountA.getId(), userA.getId(), userA.getRole());
        assertEquals(new BigDecimal("200.0000"), finalBalance, "Final balance must be exactly ₹200");
    }

    @Test
    @DisplayName("Scenario 2: Idempotency Protection — Repeated submission of the same idempotency key executes only once")
    void testIdempotentRepeatedTransfer() {
        // Fund Account A with ₹1000
        transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), new BigDecimal("1000.0000"), "INR", "Fund"),
                userA.getId(),
                userA.getRole(),
                "FUND-" + UUID.randomUUID()
        );

        String idempotencyKey = "IDEM-KEY-" + UUID.randomUUID();
        TransferRequest request = new TransferRequest(
                accountA.getAccountNumber(),
                accountB.getAccountNumber(),
                new BigDecimal("300.0000"),
                "INR",
                "Idempotent Transfer"
        );

        // First call
        TransactionDto txn1 = transactionService.transfer(request, userA.getId(), userA.getRole(), idempotencyKey);
        assertNotNull(txn1);
        assertEquals(new BigDecimal("300.0000"), txn1.getAmount());

        // Second call with same idempotency key and same payload (e.g. user double-clicked or network retried)
        TransactionDto txn2 = transactionService.transfer(request, userA.getId(), userA.getRole(), idempotencyKey);
        assertNotNull(txn2);
        assertEquals(txn1.getTransactionReference(), txn2.getTransactionReference(),
                "Subsequent idempotent calls must return the original transaction reference");

        // Verify balance was only deducted ONCE (1000 - 300 = 700, NOT 400!)
        BigDecimal balanceA = accountService.getAccountBalance(accountA.getId(), userA.getId(), userA.getRole());
        assertEquals(new BigDecimal("700.0000"), balanceA);

        BigDecimal balanceB = accountService.getAccountBalance(accountB.getId(), userB.getId(), userB.getRole());
        assertEquals(new BigDecimal("300.0000"), balanceB);
    }

    @Test
    @DisplayName("Scenario 3: Idempotency Conflict — Reusing the same key with different amount returns Conflict")
    void testIdempotencyConflictOnDifferentPayload() {
        // Fund Account A
        transactionService.deposit(
                new DepositRequest(accountA.getAccountNumber(), new BigDecimal("1000.0000"), "INR", "Fund"),
                userA.getId(),
                userA.getRole(),
                "FUND-" + UUID.randomUUID()
        );

        String idempotencyKey = "IDEM-KEY-CONFLICT-" + UUID.randomUUID();

        // Call 1: Transfer ₹100
        TransferRequest request1 = new TransferRequest(
                accountA.getAccountNumber(), accountB.getAccountNumber(),
                new BigDecimal("100.0000"), "INR", "Transfer ₹100"
        );
        transactionService.transfer(request1, userA.getId(), userA.getRole(), idempotencyKey);

        // Call 2: Same key, but amount is ₹200 (payload changed!)
        TransferRequest request2 = new TransferRequest(
                accountA.getAccountNumber(), accountB.getAccountNumber(),
                new BigDecimal("200.0000"), "INR", "Transfer ₹200"
        );

        assertThrows(IdempotencyConflictException.class, () ->
                transactionService.transfer(request2, userA.getId(), userA.getRole(), idempotencyKey));
    }
}

