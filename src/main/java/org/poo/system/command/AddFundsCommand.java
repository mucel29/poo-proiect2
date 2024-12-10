package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;

public class AddFundsCommand extends Command.Base {

    private String IBAN;
    private double amount;

    public AddFundsCommand(String IBAN, double amount) {
        super(Type.ADD_FUNDS);
        this.IBAN = IBAN;
        this.amount = amount;
    }

    @Override
    public void execute() throws UserNotFoundException, OwnershipException {
        Account targetAccount = BankingSystem.getAccount(this.IBAN);
        targetAccount.setFunds(targetAccount.getFunds() + this.amount);
    }

    public static AddFundsCommand fromNode(final JsonNode node) throws BankingInputException {
        String IBAN = node.get("account").asText();
        double amount = node.get("amount").asDouble(-1);

        if (IBAN.isEmpty() || amount < 0) {
            throw new BankingInputException("Missing account / amount for AddFunds\n" + node.toPrettyString());
        }

        return new AddFundsCommand(IBAN, amount);
    }

}