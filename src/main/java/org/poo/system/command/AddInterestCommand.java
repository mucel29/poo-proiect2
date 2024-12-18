package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.user.Account;

public class AddInterestCommand extends Command.Base {

    private final String account;

    public AddInterestCommand(final String account) {
        super(Type.ADD_INTEREST);
        this.account = account;
    }

    /**
     * {@inheritDoc}
     * @throws UserNotFoundException if no user owns the given account
     * @throws OperationException if the account is not a savings one
     */
    @Override
    public void execute() throws UserNotFoundException, OperationException {
        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);
        if (targetAccount.getAccountType() != Account.Type.SAVINGS) {
            throw new OperationException(
                    "This is not a savings account",
                    null,
                    new CommandDescriptionHandler(this)
                    );
        }

        targetAccount.setFunds(
                targetAccount.getFunds()
                        + targetAccount.getInterest()
                        * targetAccount.getInterest()
        );

    }

    /**
     * Deserializes the given node into a {@code Command.Base}  instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");

        return new AddInterestCommand(account);
    }

}
