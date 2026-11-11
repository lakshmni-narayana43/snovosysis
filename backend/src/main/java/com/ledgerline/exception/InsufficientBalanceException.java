package com.ledgerline.exception;

public class InsufficientBalanceException extends LedgerLineException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

