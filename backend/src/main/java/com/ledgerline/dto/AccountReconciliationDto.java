package com.ledgerline.dto;

import java.math.BigDecimal;

public class AccountReconciliationDto {

    private Long accountId;
    private String accountNumber;
    private BigDecimal currentBalance;
    private BigDecimal ledgerCalculatedBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private boolean consistent;

    public AccountReconciliationDto() {}

    public AccountReconciliationDto(Long accountId, String accountNumber, BigDecimal currentBalance,
                                    BigDecimal ledgerCalculatedBalance, BigDecimal totalDebits,
                                    BigDecimal totalCredits, boolean consistent) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.currentBalance = currentBalance;
        this.ledgerCalculatedBalance = ledgerCalculatedBalance;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.consistent = consistent;
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

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getLedgerCalculatedBalance() {
        return ledgerCalculatedBalance;
    }

    public void setLedgerCalculatedBalance(BigDecimal ledgerCalculatedBalance) {
        this.ledgerCalculatedBalance = ledgerCalculatedBalance;
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

    public boolean isConsistent() {
        return consistent;
    }

    public void setConsistent(boolean consistent) {
        this.consistent = consistent;
    }
}

