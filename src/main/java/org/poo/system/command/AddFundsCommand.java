package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;
import org.poo.system.user.User;

public class AddFundsCommand extends Command.Base {

    private final String account;
    private final double amount;
    private final String email;

    public AddFundsCommand(
            final String account,
            final double amount,
            final String email
    ) {
        super(Type.ADD_FUNDS);
        this.account = account;
        this.amount = amount;
        this.email = email;
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnershipException if no user owns the given account
     * @throws OperationException if the user is not authorized to deposit
     */
    @Override
    public void execute() throws OwnershipException, OperationException {
        // Retrieve the account from the storage provider
        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(this.account);
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        // Add the funds to the account
        targetAccount.authorizeDeposit(
                targetUser,
                new Amount(amount, targetAccount.getCurrency())
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
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String email = IOUtils.readStringChecked(node, "email");

        return new AddFundsCommand(account, amount, email);
    }

}
