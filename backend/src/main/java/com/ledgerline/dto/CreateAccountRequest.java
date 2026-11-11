package com.ledgerline.dto;

import com.ledgerline.enums.AccountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateAccountRequest {

    @NotNull(message = "Account type is required")
    private AccountType accountType = AccountType.SAVINGS;

    private String currency = "INR";

    @DecimalMin(value = "0.00", message = "Initial deposit cannot be negative")
    private BigDecimal initialDeposit = BigDecimal.ZERO;

    public CreateAccountRequest() {}

    public CreateAccountRequest(AccountType accountType, String currency, BigDecimal initialDeposit) {
        this.accountType = accountType;
        this.currency = currency;
        this.initialDeposit = initialDeposit;
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

    public BigDecimal getInitialDeposit() {
        return initialDeposit;
    }

    public void setInitialDeposit(BigDecimal initialDeposit) {
        this.initialDeposit = initialDeposit;
    }
}

