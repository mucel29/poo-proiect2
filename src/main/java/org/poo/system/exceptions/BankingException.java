package org.poo.system.exceptions;

import org.poo.system.BankingSystem;

public abstract class BankingException extends RuntimeException {
    public BankingException(final String message) {
        super(message);
    }

    /**
     * Handles the exception
     * @return whether the exception was handled
     */
    public boolean handle() {
        return !BankingSystem.VERBOSE_LOGGING;
    }

    /**
     * @return a detailed message of the error
     */
    public String getDetailedMessage() {
        return super.getMessage();
    }

}
