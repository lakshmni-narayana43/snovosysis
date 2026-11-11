package com.ledgerline.config;

import com.ledgerline.dto.CreateAccountRequest;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.entity.Account;
import com.ledgerline.entity.User;
import com.ledgerline.enums.AccountType;
import com.ledgerline.enums.Role;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AccountService;
import com.ledgerline.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           AccountService accountService,
                           TransactionService transactionService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionService = transactionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // 1. Ensure system clearing account exists
        if (accountRepository.findByAccountNumber("ACC-SYSTEM-CLEARING").isEmpty()) {
            Account clearing = new Account(
                    "ACC-SYSTEM-CLEARING",
                    null,
                    AccountType.SYSTEM_CLEARING,
                    "INR",
                    BigDecimal.ZERO
            );
            accountRepository.save(clearing);
            log.info("Initialized system clearing account: ACC-SYSTEM-CLEARING");
        }

        // 2. Admin User
        User admin = userRepository.findByUsername("admin").orElseGet(() -> {
            User u = new User("admin", "admin@ledgerline.com", passwordEncoder.encode("Admin@123"), "System Administrator", Role.ROLE_ADMIN);
            User saved = userRepository.save(u);
            log.info("Seeded Admin user: admin / Admin@123");
            return saved;
        });

        // 3. Auditor User
        User auditor = userRepository.findByUsername("auditor").orElseGet(() -> {
            User u = new User("auditor", "auditor@ledgerline.com", passwordEncoder.encode("Auditor@123"), "Chief Financial Auditor", Role.ROLE_AUDITOR);
            User saved = userRepository.save(u);
            log.info("Seeded Auditor user: auditor / Auditor@123");
            return saved;
        });

        // 4. Account Holder 1: Alice
        User alice = userRepository.findByUsername("alice").orElseGet(() -> {
            User u = new User("alice", "alice@ledgerline.com", passwordEncoder.encode("User@123"), "Alice Smith", Role.ROLE_ACCOUNT_HOLDER);
            User saved = userRepository.save(u);
            log.info("Seeded Account Holder: alice / User@123");
            return saved;
        });

        // 5. Account Holder 2: Bob
        User bob = userRepository.findByUsername("bob").orElseGet(() -> {
            User u = new User("bob", "bob@ledgerline.com", passwordEncoder.encode("User@123"), "Bob Jones", Role.ROLE_ACCOUNT_HOLDER);
            User saved = userRepository.save(u);
            log.info("Seeded Account Holder: bob / User@123");
            return saved;
        });

        // Check if accounts exist for Alice
        List<Account> aliceAccounts = accountRepository.findByUserId(alice.getId());
        if (aliceAccounts.isEmpty()) {
            var accDto = accountService.createAccount(alice.getId(), new CreateAccountRequest(AccountType.SAVINGS, "INR", BigDecimal.ZERO));
            // Seed opening deposit via verifiable double-entry transaction
            transactionService.deposit(
                    new DepositRequest(accDto.getAccountNumber(), new BigDecimal("10000.0000"), "INR", "Initial opening deposit"),
                    alice.getId(),
                    alice.getRole(),
                    "SEED-INIT-DEPOSIT-ALICE-1"
            );
            log.info("Seeded Alice primary account with ₹10,000 double-entry balance: {}", accDto.getAccountNumber());
        }

        // Check if accounts exist for Bob
        List<Account> bobAccounts = accountRepository.findByUserId(bob.getId());
        if (bobAccounts.isEmpty()) {
            var accDto = accountService.createAccount(bob.getId(), new CreateAccountRequest(AccountType.CHECKING, "INR", BigDecimal.ZERO));
            transactionService.deposit(
                    new DepositRequest(accDto.getAccountNumber(), new BigDecimal("5000.0000"), "INR", "Initial opening deposit"),
                    bob.getId(),
                    bob.getRole(),
                    "SEED-INIT-DEPOSIT-BOB-1"
            );
            log.info("Seeded Bob primary account with ₹5,000 double-entry balance: {}", accDto.getAccountNumber());
        }
    }
}

