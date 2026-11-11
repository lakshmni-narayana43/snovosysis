package com.ledgerline.exception;

public class IdempotencyConflictException extends LedgerLineException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}

