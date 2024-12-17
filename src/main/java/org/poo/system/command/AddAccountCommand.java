package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
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
        User targetUser = BankingSystem.getUserByEmail(this.email);

        Account newAccount = new Account(
                targetUser,
                Utils.generateIBAN(),
                this.currency,
                this.accountType
        );
        if (this.accountType == Account.Type.SAVINGS) {
            newAccount.setInterest(this.interest);
        }

        // Add the new account into the map and to the user
        BankingSystem.getInstance().getAccountMap().put(newAccount.getAccountIBAN(), targetUser);
        targetUser.getAccounts().add(newAccount);
        newAccount.getTransactions().add(new Transaction.Base("New account created", timestamp));
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws BankingInputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws BankingInputException {

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
