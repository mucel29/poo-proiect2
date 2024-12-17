package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
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
     * @throws OwnershipException if no user owns the given account
     */
    @Override
    public void execute() throws UserNotFoundException, OwnershipException {
        Account targetAccount = BankingSystem.getAccount(account);
        targetAccount.setMinBalance(amount);
    }
    /**
     * Deserializes the given node into a `Command.Base` instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws BankingInputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws BankingInputException {
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String account = IOUtils.readStringChecked(node, "account");

        return new MinBalanceCommand(account, amount);
    }
}
