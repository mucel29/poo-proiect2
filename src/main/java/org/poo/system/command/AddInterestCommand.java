package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;

public class AddInterestCommand extends Command.Base {

    private final String account;

    public AddInterestCommand(final String account) {
        super(Type.ADD_INTEREST);
        this.account = account;
    }

    /**
     * @throws UserNotFoundException if no user owns the given account
     * @throws OperationException if the account is not a savings one
     */
    @Override
    public void execute() throws UserNotFoundException, OperationException {
        Account targetAccount = BankingSystem.getAccount(account);
        if (targetAccount.getAccountType() != Account.Type.SAVINGS) {
            super.output((root) -> {
                root.put("description", "This is not a savings account");
                root.put("timestamp", timestamp);
            });
            throw new OperationException("Target account is not a savings account");
        }

        targetAccount.setFunds(
                targetAccount.getFunds()
                        + targetAccount.getInterest()
                        * targetAccount.getInterest()
        );

    }

    /**
     * Deserializes the given node into a `Command.Base` instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws BankingInputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws BankingInputException {
        String account = IOUtils.readStringChecked(node, "account");

        return new AddInterestCommand(account);
    }

}
