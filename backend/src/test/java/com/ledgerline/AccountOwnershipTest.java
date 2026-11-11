package com.ledgerline;

import com.ledgerline.entity.Account;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.Role;
import com.ledgerline.exception.ForbiddenException;
import com.ledgerline.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AccountOwnershipTest {

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        // Pure unit testing of domain authorization rules without mocking overhead
        accountService = new AccountService(null, null, null);
    }

    @Test
    @DisplayName("Owner can access their own account")
    void testOwnerCanAccessOwnAccount() {
        User user = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        user.setId(10L);

        Account account = new Account("ACC-101", user, AccountType.SAVINGS, "INR", BigDecimal.valueOf(500));

        assertDoesNotThrow(() ->
                accountService.verifyOwnershipOrStaff(account, 10L, Role.ROLE_ACCOUNT_HOLDER));
    }

    @Test
    @DisplayName("Unauthorized user accessing another user's account throws ForbiddenException (Scenario 5)")
    void testUnauthorizedAccessThrowsForbidden() {
        User alice = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        alice.setId(10L);

        Account account = new Account("ACC-101", alice, AccountType.SAVINGS, "INR", BigDecimal.valueOf(500));

        Long maliciousUserId = 99L;
        assertThrows(ForbiddenException.class, () ->
                accountService.verifyOwnershipOrStaff(account, maliciousUserId, Role.ROLE_ACCOUNT_HOLDER));
    }

    @Test
    @DisplayName("Administrator can access any account")
    void testAdminCanAccessAnyAccount() {
        User alice = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        alice.setId(10L);

        Account account = new Account("ACC-101", alice, AccountType.SAVINGS, "INR", BigDecimal.valueOf(500));

        Long adminUserId = 1L;
        assertDoesNotThrow(() ->
                accountService.verifyOwnershipOrStaff(account, adminUserId, Role.ROLE_ADMIN));
    }

    @Test
    @DisplayName("Auditor can access any account for audit purposes")
    void testAuditorCanAccessAnyAccount() {
        User alice = new User("alice", "alice@example.com", "hash", "Alice", Role.ROLE_ACCOUNT_HOLDER);
        alice.setId(10L);

        Account account = new Account("ACC-101", alice, AccountType.SAVINGS, "INR", BigDecimal.valueOf(500));

        Long auditorUserId = 2L;
        assertDoesNotThrow(() ->
                accountService.verifyOwnershipOrStaff(account, auditorUserId, Role.ROLE_AUDITOR));
    }
}
