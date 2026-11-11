package com.ledgerline.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class LedgerVerificationResult {

    private long totalTransactions;
    private long totalLedgerEntries;
    private BigDecimal totalDebits = BigDecimal.ZERO;
    private BigDecimal totalCredits = BigDecimal.ZERO;
    private BigDecimal imbalance = BigDecimal.ZERO;
    private boolean balanced;
    private List<String> issues = new ArrayList<>();
    private List<AccountReconciliationDto> accountReconciliations = new ArrayList<>();
    private OffsetDateTime verifiedAt;

    public LedgerVerificationResult() {
        this.verifiedAt = OffsetDateTime.now();
    }

    public long getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(long totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public long getTotalLedgerEntries() {
        return totalLedgerEntries;
    }

    public void setTotalLedgerEntries(long totalLedgerEntries) {
        this.totalLedgerEntries = totalLedgerEntries;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public BigDecimal getImbalance() {
        return imbalance;
    }

    public void setImbalance(BigDecimal imbalance) {
        this.imbalance = imbalance;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public void addIssue(String issue) {
        this.issues.add(issue);
    }

    public List<AccountReconciliationDto> getAccountReconciliations() {
        return accountReconciliations;
    }

    public void setAccountReconciliations(List<AccountReconciliationDto> accountReconciliations) {
        this.accountReconciliations = accountReconciliations;
    }

    public OffsetDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(OffsetDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
}

