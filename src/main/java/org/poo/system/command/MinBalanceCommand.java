package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;

public class MinBalanceCommand extends Command.Base {

    private final double amount;
    private final String account;

    public MinBalanceCommand(
            final String account,
            final double amount
    ) {
        super(Command.Type.SET_MIN_BALANCE);
        this.amount = amount;
        this.account = account;
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnershipException if no user owns the given account
     */
    @Override
    public void execute() throws OwnershipException {
        // Retrieve the account from the storage provider
        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);

        // Set the new minimum balance
        targetAccount.setMinBalance(amount);
    }
    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String account = IOUtils.readStringChecked(node, "account");

        return new MinBalanceCommand(account, amount);
    }
}
