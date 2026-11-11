package com.ledgerline.dto;

import com.ledgerline.entity.LedgerEntry;
import com.ledgerline.enums.EntryType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class LedgerEntryDto {

    private Long id;
    private Long transactionId;
    private String transactionReference;
    private Long accountId;
    private String accountNumber;
    private EntryType entryType;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime createdAt;

    public LedgerEntryDto() {}

    public LedgerEntryDto(LedgerEntry entry) {
        if (entry != null) {
            this.id = entry.getId();
            if (entry.getTransaction() != null) {
                this.transactionId = entry.getTransaction().getId();
                this.transactionReference = entry.getTransaction().getTransactionReference();
            }
            if (entry.getAccount() != null) {
                this.accountId = entry.getAccount().getId();
                this.accountNumber = entry.getAccount().getAccountNumber();
            }
            this.entryType = entry.getEntryType();
            this.amount = entry.getAmount();
            this.currency = entry.getCurrency();
            this.createdAt = entry.getCreatedAt();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

