package org.poo.system.exceptions.handlers;

public interface ExceptionHandler {

    /**
     * Called by a BankingException if it was provided to it
     * @param message the message to display in the output
     */
    void handle(String message);

}
