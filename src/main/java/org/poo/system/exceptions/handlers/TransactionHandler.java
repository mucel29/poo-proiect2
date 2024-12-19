package org.poo.system.exceptions.handlers;

import org.poo.system.Transaction;
import org.poo.system.user.Account;

/**
 * An exception handler that generates an erroneous transaction
 * with a description containing the exception message
 */
public final class TransactionHandler implements ExceptionHandler {

    private final Account targetAccount;
    private final int timestamp;
    private final String message;

    public TransactionHandler(
            final Account targetAccount,
            final int timestamp
    ) {
        this.targetAccount = targetAccount;
        this.timestamp = timestamp;
        this.message = null;
    }

    public TransactionHandler(
            final Account targetAccount,
            final int timestamp,
            final String message
    ) {
        this.targetAccount = targetAccount;
        this.timestamp = timestamp;
        this.message = message;
    }

    /**
     * Called by a BankingException if it was provided to it
     *
     * @param exceptionMessage the message to display in the output
     */
    @Override
    public void handle(final String exceptionMessage) {
        targetAccount
                .getTransactions()
                .add(new Transaction.Base(
                        message == null ? exceptionMessage : message, timestamp
                ));
    }
}
