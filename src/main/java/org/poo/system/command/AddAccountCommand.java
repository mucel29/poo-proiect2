package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
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
        if (targetUser == null) {
            return; // ??? output
        }

        // We know the currency is supported, otherwise there would've been an Exception already

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

        String email = node.get("email").asText();

        // TODO: verify that the currency is registered [verifyCurrency that returns String ig]

        // Can't verify currency here, first tests don't have exchange rates to register them
        String currency = node.get("currency").asText();

        // Just add it ig
        Exchange.registerCurrency(currency);

        // This will throw an exception if it's empty anyway
        Account.Type accountType = Account.Type.fromString(node.get("accountType").asText());

        if (email.isEmpty()) {
            throw new BankingInputException("Missing email for AddAccount\n" + node.toPrettyString());
        }

        double interest = 0;
        if (accountType == Account.Type.SAVINGS) {
            if (node.get("interestRate") == null) {
                throw new BankingInputException("Missing interest for savings account\n" + node.toPrettyString());
            }
            interest = node.get("interestRate").asDouble();
        }

        return new AddAccountCommand(email, currency, accountType, interest);
    }

}
