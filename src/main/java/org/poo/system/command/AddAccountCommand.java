package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.exchange.Exchange;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.User;
import org.poo.utils.Utils;

public class AddAccountCommand extends Command.Base {

    private String email;
    private String currency;
    private Account.Type accountType;
    private double interest;

    public AddAccountCommand(String email, String currency, Account.Type accountType, double interest) {
        super(Command.Type.ADD_ACCOUNT);
        this.email = email;
        this.currency = currency;
        this.accountType = accountType;
        this.interest = interest;
    }

    @Override
    public void execute() throws UserNotFoundException {

        // Get user by using the email
        User targetUser = BankingSystem.getUserByEmail(this.email);

        Account newAccount = new Account(targetUser, Utils.generateIBAN(), this.currency, this.accountType);
        if (this.accountType == Account.Type.SAVINGS) {
            newAccount.setInterest(this.interest);
        }

        // Add the new account into the map and to the user
        BankingSystem.getInstance().getAccountMap().put(newAccount.getIBAN(), targetUser);
        targetUser.getAccounts().add(newAccount);
        newAccount.getTransactions().add(new Transaction.Base("New account created", timestamp));
    }

    public static AddAccountCommand fromNode(JsonNode node) throws BankingInputException {

        String email = IOUtils.readStringChecked(node, "email");
        String currency = IOUtils.readStringChecked(node, "currency");

        Exchange.registerCurrency(currency);

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
