package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.User;
import org.poo.utils.Utils;

public class AddAccountCommand extends Command.Base {

    private final String email;
    private final String currency;
    private final Account.Type accountType;
    private final double interest;

    public AddAccountCommand(
            final String email,
            final String currency,
            final Account.Type accountType,
            final double interest
    ) {
        super(Command.Type.ADD_ACCOUNT);
        this.email = email;
        this.currency = currency;
        this.accountType = accountType;
        this.interest = interest;
    }

    /**
     * {@inheritDoc}
     * @throws UserNotFoundException if the user could not be found
     */
    @Override
    public void execute() throws UserNotFoundException {

        // Get user by using the email
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(this.email);

        // Create the account
        Account newAccount = new Account(
                targetUser,
                Utils.generateIBAN(),
                this.currency,
                this.accountType
        );

        // If the requested account is a savings one, add the interest
        if (this.accountType == Account.Type.SAVINGS) {
            newAccount.setInterest(this.interest);
        }

        // Register the created account
        BankingSystem.getStorageProvider().registerAccount(newAccount);

        // Add creation transaction to the account
        newAccount.getTransactions().add(new Transaction.Base("New account created", timestamp));
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {

        String email = IOUtils.readStringChecked(node, "email");
        String currency = IOUtils.readStringChecked(node, "currency");

        BankingSystem.getExchangeProvider().registerCurrency(currency);

        Account.Type accountType = Account.Type.fromString(
                IOUtils.readStringChecked(node, "accountType")
        );


        double interest = 0;
        if (accountType == Account.Type.SAVINGS) {
            interest = IOUtils.readDoubleChecked(node, "interestRate");
        }

        return new AddAccountCommand(email, currency, accountType, interest);
    }

}
