package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.user.Account;

public class ChangeInterestCommand extends Command.Base {

    private final String account;
    private final double newRate;

    public ChangeInterestCommand(
            final String account,
            final double newRate
    ) {
        super(Type.CHANGE_INTEREST_RATE);
        this.account = account;
        this.newRate = newRate;
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnershipException if no user owns the given account
     * @throws OperationException if the account is not a savings one
     */
    @Override
    public void execute() throws OwnershipException, OperationException {
        // Retrieve the account from the storage provider
        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);

        // Generate error if the account is not a savings one
        if (targetAccount.getAccountType() != Account.Type.SAVINGS) {
            throw new OperationException(
                    "This is not a savings account",
                    null,
                    new CommandDescriptionHandler(this)
            );
        }

        // Add a transaction for the rate change
        targetAccount
                .getTransactions()
                .add(new Transaction.Base(
                        "Interest rate of the account changed to " + newRate,
                        timestamp
                ));

        // Change the rate
        targetAccount.setInterest(newRate);

    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        double newInterestRate = IOUtils.readDoubleChecked(node, "interestRate");

        return new ChangeInterestCommand(account, newInterestRate);
    }

}
