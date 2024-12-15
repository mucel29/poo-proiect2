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
    private final String IBAN;

    public MinBalanceCommand(String IBAN, double amount) {
        super(Command.Type.SET_MIN_BALANCE);
        this.amount = amount;
        this.IBAN = IBAN;
    }

    @Override
    public void execute() throws UserNotFoundException, OwnershipException {
        Account targetAccount = BankingSystem.getAccount(IBAN);
        targetAccount.setMinBalance(amount);
    }

    public static MinBalanceCommand fromNode(final JsonNode node) throws BankingInputException {
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String account = IOUtils.readStringChecked(node, "account");
        
        return new MinBalanceCommand(account, amount);
    }
}
