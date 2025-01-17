package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;

public class AddFundsCommand extends Command.Base {

    private final String account;
    private final double amount;

    public AddFundsCommand(
            final String account,
            final double amount
    ) {
        super(Type.ADD_FUNDS);
        this.account = account;
        this.amount = amount;
    }

    /**
     * {@inheritDoc}
     * @throws OwnershipException no user owns the given account
     */
    @Override
    public void execute() throws OwnershipException {
        // Retrieve the account from the storage provider
        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(this.account);

        // Add the funds to the account
        targetAccount.setFunds(targetAccount.getFunds().add(this.amount));
    }

    /**
     * Deserializes the given node into a {@code Command.Base}  instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        double amount = IOUtils.readDoubleChecked(node, "amount");

        return new AddFundsCommand(account, amount);
    }

}
