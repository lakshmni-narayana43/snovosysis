package com.ledgerline.dto;

import com.ledgerline.entity.Transaction;
import com.ledgerline.enums.TransactionStatus;
import com.ledgerline.enums.TransactionType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionDto {

    private Long id;
    private String transactionReference;
    private TransactionType transactionType;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private String initiatedByUsername;
    private String idempotencyKey;
    private String failureReason;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
    private List<LedgerEntryDto> ledgerEntries = new ArrayList<>();

    public TransactionDto() {}

    public TransactionDto(Transaction transaction) {
        if (transaction != null) {
            this.id = transaction.getId();
            this.transactionReference = transaction.getTransactionReference();
            this.transactionType = transaction.getTransactionType();
            this.status = transaction.getStatus();
            this.amount = transaction.getAmount();
            this.currency = transaction.getCurrency();
            this.description = transaction.getDescription();
            if (transaction.getSourceAccount() != null) {
                this.sourceAccountNumber = transaction.getSourceAccount().getAccountNumber();
            }
            if (transaction.getDestinationAccount() != null) {
                this.destinationAccountNumber = transaction.getDestinationAccount().getAccountNumber();
            }
            if (transaction.getInitiatedBy() != null) {
                this.initiatedByUsername = transaction.getInitiatedBy().getUsername();
            }
            this.idempotencyKey = transaction.getIdempotencyKey();
            this.failureReason = transaction.getFailureReason();
            this.createdAt = transaction.getCreatedAt();
            this.completedAt = transaction.getCompletedAt();
            if (transaction.getLedgerEntries() != null) {
                this.ledgerEntries = transaction.getLedgerEntries().stream()
                        .map(LedgerEntryDto::new)
                        .toList();
            }
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceAccountNumber() {
        return sourceAccountNumber;
    }

    public void setSourceAccountNumber(String sourceAccountNumber) {
        this.sourceAccountNumber = sourceAccountNumber;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public String getInitiatedByUsername() {
        return initiatedByUsername;
    }

    public void setInitiatedByUsername(String initiatedByUsername) {
        this.initiatedByUsername = initiatedByUsername;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(OffsetDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<LedgerEntryDto> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<LedgerEntryDto> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }
}

