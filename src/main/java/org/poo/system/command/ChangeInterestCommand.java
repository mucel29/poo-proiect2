package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.user.Account;

public class ChangeInterestCommand extends Command.Base {

    private final String account;
    private final double newRate;

    public ChangeInterestCommand(String account, double newRate) {
        super(Type.CHANGE_INTEREST_RATE);
        this.account = account;
        this.newRate = newRate;
    }

    @Override
    public void execute() {
        Account targetAccount = BankingSystem.getAccount(account);
        if (targetAccount.getAccountType() != Account.Type.SAVINGS) {
            super.output((root) -> {
                root.put("description", "This is not a savings account");
                root.put("timestamp", timestamp);
            });
            throw new OperationException("Target account is not a savings account");
        }

        targetAccount
                .getTransactions()
                .add(new Transaction.Base(
                        "Interest rate of the account changed to " + newRate, timestamp
                ));

        targetAccount.setInterest(newRate);

    }

    public static ChangeInterestCommand fromNode(final JsonNode node) throws BankingInputException {
        String account = node.get("account").asText();
        double newInterestRate = node.get("interestRate").asDouble(-1);

        if (account.isEmpty() || newInterestRate < 0) {
            throw new BankingInputException("Missing argument for ChangeInterest\n" + node.toPrettyString());
        }

        return new ChangeInterestCommand(account, newInterestRate);
    }

}
