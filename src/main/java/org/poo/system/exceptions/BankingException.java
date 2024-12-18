package org.poo.system.exceptions;

public abstract class BankingException extends RuntimeException {
    public BankingException(final String message) {
        super(message);
    }

    /**
     * Handles the exception
     * @return whether the exception was handled
     */
    public abstract boolean handle();

    /**
     * @return a detailed message of the error
     */
    public String getDetailedMessage() {
        return super.getMessage();
    }

}
