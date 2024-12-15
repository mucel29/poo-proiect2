package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.user.Account;

public class AddInterestCommand extends Command.Base {

    private final String account;

    public AddInterestCommand(String account) {
        super(Type.ADD_INTEREST);
        this.account = account;
    }

    @Override
    public void execute()
    {
        Account targetAccount = BankingSystem.getAccount(account);
        if (targetAccount.getAccountType() != Account.Type.SAVINGS) {
            super.output((root) -> {
                root.put("description", "This is not a savings account");
                root.put("timestamp", timestamp);
            });
            throw new OperationException("Target account is not a savings account");
        }

        targetAccount.setFunds(targetAccount.getFunds() + targetAccount.getInterest() * targetAccount.getInterest());

    }

    public static AddInterestCommand fromNode(final JsonNode node) throws BankingInputException {
        String account = IOUtils.readStringChecked(node, "account");

        return new AddInterestCommand(account);
    }

}
