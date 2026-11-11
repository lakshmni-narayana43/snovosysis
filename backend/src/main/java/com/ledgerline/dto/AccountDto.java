package com.ledgerline.dto;

import com.ledgerline.entity.Account;
import com.ledgerline.enums.AccountStatus;
import com.ledgerline.enums.AccountType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class AccountDto {

    private Long id;
    private String accountNumber;
    private Long userId;
    private String username;
    private AccountType accountType;
    private String currency;
    private BigDecimal balance;
    private AccountStatus status;
    private OffsetDateTime createdAt;

    public AccountDto() {}

    public AccountDto(Account account) {
        if (account != null) {
            this.id = account.getId();
            this.accountNumber = account.getAccountNumber();
            if (account.getUser() != null) {
                this.userId = account.getUser().getId();
                this.username = account.getUser().getUsername();
            }
            this.accountType = account.getAccountType();
            this.currency = account.getCurrency();
            this.balance = account.getBalance();
            this.status = account.getStatus();
            this.createdAt = account.getCreatedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public AccountType getAccountType() {
        return accountType;
    }

    public void setAccountType(AccountType accountType) {
        this.accountType = accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

