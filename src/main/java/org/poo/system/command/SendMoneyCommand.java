package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;
import org.poo.utils.Utils;

public class SendMoneyCommand extends Command.Base {

    private final String sender;
    private final String receiver;
    private final double amount;
    private final String description;

    public SendMoneyCommand(String sender, String receiver, double amount, String description) {
        super(Type.SEND_MONEY);
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.description = description;
    }


    @Override
    public void execute() throws OwnershipException, OperationException {
        // Todo: implement alias matching, Transactions
        // TODO: Add reverse exchange: if we have EUR -> USD, we should also have USD -> EUR
        Account senderAccount = Utils.verifyIBAN(sender)
                ? BankingSystem.getAccount(sender)
                : BankingSystem.getByAlias(sender);
        Account receiverAccount = Utils.verifyIBAN(receiver)
                ? BankingSystem.getAccount(receiver)
                : BankingSystem.getByAlias(receiver);

        double convertedAmount = amount * BankingSystem.getExchangeRate(senderAccount.getCurrency(), receiverAccount.getCurrency());

        if (senderAccount.getFunds() < amount) {
            throw new OperationException("Not enough balance: " + senderAccount.getFunds() + " (wanted to send " + amount + " " + senderAccount.getCurrency() + ") [" + convertedAmount + " " + receiverAccount.getCurrency() + "]");
        }

        senderAccount.setFunds(senderAccount.getFunds() - amount);
        receiverAccount.setFunds(receiverAccount.getFunds() + convertedAmount);

        System.out.println("Sent " + amount + " " + senderAccount.getCurrency() + " (" + convertedAmount + " " + receiverAccount.getCurrency() + ") to " + receiverAccount.getIBAN());

    }

    public static SendMoneyCommand fromNode(final JsonNode node) throws BankingInputException {
        String sender = node.get("account").asText();
        String receiver = node.get("receiver").asText();
        double amount = node.get("amount").asDouble(-1);
        String description = node.get("description").asText();

        if (sender.isEmpty() || receiver.isEmpty() || description.isEmpty() || amount < 0) {
            throw new BankingInputException("Missing arguments for SendMoney\n" + node.toPrettyString());
        }

        return new SendMoneyCommand(sender, receiver, amount, description);
    }

}
