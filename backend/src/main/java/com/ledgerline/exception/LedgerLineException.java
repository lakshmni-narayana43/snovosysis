package com.ledgerline.exception;

public class LedgerLineException extends RuntimeException {
    public LedgerLineException(String message) {
        super(message);
    }

    public LedgerLineException(String message, Throwable cause) {
        super(message, cause);
    }
}

