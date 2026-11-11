# LedgerLine — Money Movement With a Ledger You Can Prove

LedgerLine is a secure, production-grade financial transaction management system engineered with database-level transaction guarantees, deterministic concurrency protection, idempotency enforcement, and a mathematically verifiable double-entry ledger.

Financial correctness is prioritized over simple CRUD operations. Money is never created or destroyed during internal transfers, concurrent withdrawals are strictly serialized to prevent overdrafts, duplicate requests are rejected or safely replayed, and every movement of funds is recorded in an immutable audit trail.

---

## 1. Core Financial Principles & Invariants

| Invariant | Mathematical Formulation | Enforcement Mechanism |
| :--- | :--- | :--- |
| **Money Conservation** | $\sum \Delta \text{Balance}_{\text{Sender}} + \sum \Delta \text{Balance}_{\text{Receiver}} = 0$ | Atomic `@Transactional` transfer with paired debit/credit |
| **Double-Entry Balance** | $\sum \text{Debits} - \sum \text{Credits} = 0$ | Dual ledger records written within the same database transaction |
| **Account Reconciliation** | $\text{Balance}(A) = \sum \text{Credits}(A) - \sum \text{Debits}(A)$ | Continuous verification comparing cached balance to ledger history |
| **Non-Negative Balances** | $\text{Balance}(A) \ge 0$ | Row-level checks and PostgreSQL `CHECK (balance >= 0)` |
| **Request Idempotency** | $f(\text{Key}, P) = f(\text{Key}, P)$ for all repeated executions | SHA-256 payload hashing and atomic status transitions |
| **Deadlock Freedom** | $\text{Acquisition Order} = \text{sort}(\text{Account ID}_1, \text{Account ID}_2)$ | Deterministic ascending ID locking order |

---

## 2. System Architecture

LedgerLine is structured in clear architectural layers:

```
┌────────────────────────────────────────────────────────┐
│                   React 18 + Vite UI                   │
│   (Account Holder Portal, Admin Dashboard, Auditor UI) │
└───────────────────────────┬────────────────────────────┘
                            │ REST / JSON (JWT + Idempotency Key)
┌───────────────────────────▼────────────────────────────┐
│                   Spring Boot 3.4.5                    │
│                                                        │
│  Controllers: Auth, Accounts, Transactions, Admin, etc │
│  Security: JWT Filter, BCrypt, Role-Based Access       │
│  Services: TransactionService, LedgerService, Audit... │
│  Concurrency: Pessimistic Locking + Entity Refresh     │
│  Idempotency Engine: SHA-256 Payload Hash Verification │
└───────────────────────────┬────────────────────────────┘
                            │ JPA / Hibernate ORM
┌───────────────────────────▼────────────────────────────┐
│             PostgreSQL / H2 Compatible DB              │
│  Tables: users, accounts, transactions, ledger_entries │
│          idempotency_records, audit_logs               │
└────────────────────────────────────────────────────────┘
```

---

## 3. Concurrency Protection & Race Condition Prevention

### The Problem: Simultaneous Withdrawals
When two threads concurrently withdraw ₹800 from an account holding ₹1,000, naive implementations suffer from a **Lost Update anomaly**, allowing both withdrawals to succeed and resulting in an illegal balance of -₹600.

### The LedgerLine Solution:
1. **Pessimistic Row-Level Locking (`PESSIMISTIC_WRITE`)**:
   - Queries `SELECT * FROM accounts WHERE id = ? FOR UPDATE`.
   - Forces incoming concurrent transactions to queue until the active transaction completes.
2. **First-Level Cache Invalidation (`entityManager.refresh`)**:
   - When Thread 2 acquires the row lock after Thread 1 commits, Hibernate's session cache might hold the stale balance (₹1,000).
   - Calling `entityManager.refresh(account, LockModeType.PESSIMISTIC_WRITE)` forces Hibernate to reload the latest database state, ensuring Thread 2 sees the new balance of ₹200 and immediately throws `InsufficientBalanceException`.
3. **Deterministic Deadlock Prevention**:
   - During peer-to-peer transfers involving two accounts (Account $A$ and Account $B$), concurrent transfers ($A \to B$ and $B \to A$) can cause a database deadlock if locks are acquired haphazardly.
   - LedgerLine sorts account IDs and locks the lower ID first, followed by the higher ID:
     ```java
     Long firstId = Math.min(sender.getId(), receiver.getId());
     Long secondId = Math.max(sender.getId(), receiver.getId());
     accountRepository.findByIdForUpdate(firstId);
     accountRepository.findByIdForUpdate(secondId);
     ```

---

## 4. Idempotency Engine

Network timeouts and user double-clicks often cause duplicate payment submissions. LedgerLine implements a robust idempotency protocol:

1. **Header Requirement**: Clients provide an `Idempotency-Key` HTTP header (UUID v4).
2. **Payload Fingerprinting**: The backend computes a SHA-256 digest of the request payload:
   $$\text{Hash} = \text{SHA-256}(\text{Method} + \text{URI} + \text{RequestBody})$$
3. **State Machine**:
   - **New Request**: An `idempotency_records` row is created with status `IN_PROGRESS`.
   - **Execution Completed**: The record is updated with `status = COMPLETED`, the HTTP status code, and the serialized response body.
   - **Identical Replay**: If a completed key is received with the exact same payload hash, LedgerLine bypasses transaction execution and returns the original response from cache.
   - **Payload Tampering**: If the same key is reused with a different payload (e.g., altered amount or recipient), the server rejects the request with **HTTP 409 Conflict**.

---

## 5. Double-Entry Accounting Model

Money cannot appear from nowhere or vanish into thin air. Every financial transaction generates matched ledger entries:

- **Deposit (₹1,000 into Alice's account)**:
  - `DEBIT`: Alice's Account (Asset increases $\to$ credited in internal liability balance)
  - `CREDIT`: System Clearing Account (`LL-SYSTEM-CLEARING-001`)
- **Withdrawal (₹500 from Alice's account)**:
  - `DEBIT`: Alice's Account
  - `CREDIT`: System Clearing Account (`LL-SYSTEM-CLEARING-001`)
- **Transfer (₹300 from Alice to Bob)**:
  - `DEBIT`: Alice's Account (`-₹300.0000`)
  - `CREDIT`: Bob's Account (`+₹300.0000`)
  - Net system balance change: **₹0.0000**

### Auditor Verification
The `/api/auditor/verify` endpoint computes the global trial balance in real time:
$$\text{Imbalance} = \sum_{e \in \text{Ledger}} \text{Debits}(e) - \sum_{e \in \text{Ledger}} \text{Credits}(e)$$
If $\text{Imbalance} \neq 0$, an alert is raised immediately.

---

## 6. Pre-Seeded Demo Credentials

| Role | Username | Password | Purpose |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `Admin@123` | System oversight, account freeze/unfreeze controls, audit inspection |
| **Auditor** | `auditor` | `Auditor@123` | Read-only ledger verification, trial balance audits, reconciliation |
| **Account Holder** | `alice` | `User@123` | Pre-funded with **₹10,000.0000** for live demo transfers |
| **Account Holder** | `bob` | `User@123` | Pre-funded with **₹5,000.0000** for receiving transfers |

---

## 7. REST API Catalog

### Authentication (`/api/auth`)
- `POST /api/auth/register` — Register a new account holder.
- `POST /api/auth/login` — Authenticate and receive a JWT Bearer token.
- `GET /api/auth/me` — Retrieve the current user's authenticated profile.

### Accounts (`/api/accounts`)
- `GET /api/accounts` — List accounts owned by the authenticated user.
- `POST /api/accounts` — Create an additional financial account.
- `GET /api/accounts/{id}` — Fetch details of a specific owned account.

### Transactions (`/api/transactions`)
- `POST /api/transactions/deposit` — Deposit money into an active owned account.
- `POST /api/transactions/withdraw` — Withdraw funds (guaranteed no overdraft).
- `POST /api/transactions/transfer` — Peer-to-peer transfer with double-entry ledger entries.
- `GET /api/transactions` — Paginated transaction history for the authenticated user.
- `GET /api/transactions/{ref}/ledger` — Inspect paired debit/credit ledger records for a transaction.

### Administration (`/api/admin`)
- `GET /api/admin/users` — List all registered users.
- `GET /api/admin/accounts` — List all financial accounts across all users.
- `PATCH /api/admin/accounts/{id}/status` — Freeze or unfreeze an account.
- `GET /api/admin/stats` — System telemetry (total users, accounts, transactions).

### Auditor & Compliance (`/api/auditor` & `/api/audit-logs`)
- `GET /api/auditor/verify` — Execute mathematical trial balance audit ($\sum Debits == \sum Credits$).
- `GET /api/auditor/reports` — Generate comprehensive ledger reconciliation report.
- `GET /api/auditor/ledger` — Browse the append-only global ledger journal.
- `GET /api/auditor/transactions` — Inspect all system transaction envelopes.
- `GET /api/audit-logs` — Append-only security audit trail (actors, actions, IPs, statuses).

---

## 8. Development & Quick Start

### Prerequisites
- **Java 21** or later (tested and verified on Java 21/26)
- **Apache Maven 3.9+**
- **Node.js v18+** & **npm**

### Backend Setup
```powershell
cd backend
$env:JWT_SECRET = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
mvn clean compile
mvn test
mvn spring-boot:run
```
Set `JWT_SECRET` to a unique, base64-encoded key of at least 32 random bytes in each environment; keep it private and use the same value across backend instances. The backend starts on `http://localhost:8080`.

### Frontend Setup
```powershell
cd frontend
npm install
npm run build
npm run dev
```
The React application starts on `http://localhost:5173`.

---

## 9. Verification & Automated Test Suite

The backend contains a rigorous automated test suite covering financial invariants, multithreaded concurrency, and edge cases:

```
Results :
Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- **`AccountOwnershipTest`**: Verifies that users cannot deposit, withdraw, or view accounts they do not own (HTTP 403 / Access Denied).
- **`ConcurrencyAndIdempotencyIntegrationTest`**:
  - `testConcurrentWithdrawalsRaceCondition`: Launches concurrent threads attempting simultaneous ₹800 withdrawals from a ₹1,000 balance. Proves exactly 1 succeeds and 1 fails with zero overdraft.
  - `testTransferIdempotency`: Executes repeated transfers with the same idempotency key; verifies the transaction executes only once.
  - `testIdempotencyPayloadMismatchConflict`: Reuses an idempotency key with modified payload; verifies HTTP 409 Conflict.
- **`FinancialTransactionsIntegrationTest`**:
  - `testDepositWorkflow`: Validates deposit balance updates and ledger entries.
  - `testWithdrawalInsufficientFunds`: Rejects withdrawals exceeding available balance.
  - `testTransferMoneyConservationAndLedgerTrialBalance`: Proves Sender Debit == Receiver Credit and net system change is zero.
  - `testTransferFromFrozenAccountRejected`: Rejects transfers when an account is frozen by an administrator.
- **`LedgerMathUnitTest`**: Validates double-entry mathematical invariants and rejects unbalanced entries.

---

## 10. Live Company Demonstration Script

1. **Sign In as Alice (`alice` / `User@123`)**:
   - Notice pre-funded balance of ₹10,000.0000.
   - Navigate to **Transfer**, input Bob's account number (or select Bob from demo helpers), transfer ₹1,500.0000.
   - Notice the atomic update, instant reference generation, and transaction confirmation.
2. **Review the Double-Entry Ledger**:
   - Click **Statements** $\to$ click on the transaction reference.
   - Observe the paired **DEBIT** (-₹1,500) and **CREDIT** (+₹1,500) entries.
3. **Demonstrate Overdraft Protection**:
   - Go to **Withdraw**, attempt to withdraw ₹20,000.0000.
   - Observe strict rejection: *Insufficient funds. Available balance is lower than requested withdrawal amount.*
4. **Demonstrate Administrator Controls**:
   - Sign in as `admin` (`Admin@123`).
   - Locate Alice's account, click **Freeze Account**.
   - Sign back in as Alice, attempt a transfer: transaction is immediately rejected due to frozen account status.
5. **Demonstrate Auditor Trial Balance Audit**:
   - Sign in as `auditor` (`Auditor@123`).
   - Click **Run Mathematical Integrity Audit**.
   - Verify green status: **DOUBLE-ENTRY TRIAL BALANCE: VERIFIED BALANCED** ($\sum Debits - \sum Credits = 0$).
   - Inspect the **Account Reconciliation Table** to see that every account's cached balance matches the sum of its immutable ledger entries.
