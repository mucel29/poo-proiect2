package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exchange.Exchange;
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
        if (!Utils.verifyIBAN(sender)) {
            throw new OperationException("Invalid sender IBAN [can't use an user as alias]");
        }

        Account senderAccount = BankingSystem.getAccount(sender);
        Account receiverAccount = Utils.verifyIBAN(receiver)
                ? BankingSystem.getAccount(receiver)
                : BankingSystem.getByAlias(receiver);

        double convertedAmount = amount * Exchange.getRate(senderAccount.getCurrency(), receiverAccount.getCurrency());

        if (senderAccount.getFunds() < amount) {
            throw new OperationException("Not enough balance: " + senderAccount.getFunds() + " (wanted to send " + amount + " " + senderAccount.getCurrency() + ") [" + convertedAmount + " " + receiverAccount.getCurrency() + "]");
        }

        Transaction.Transfer transaction = new Transaction.Transfer(description, timestamp)
                .setSenderIBAN(senderAccount.getIBAN())
                .setReceiverIBAN(receiverAccount.getIBAN())
                .setAmount(amount)
                .setCurrency(senderAccount.getCurrency())
                .setTransferType(Transaction.TransferType.SENT);

        senderAccount.setFunds(senderAccount.getFunds() - amount);
        senderAccount.getOwner().getTransactions().add(transaction.clone());

        receiverAccount.setFunds(receiverAccount.getFunds() + convertedAmount);
        receiverAccount.getOwner().getTransactions().add(
                transaction
                        .setCurrency(receiverAccount.getCurrency())
                        .setAmount(convertedAmount)
                        .setTransferType(Transaction.TransferType.RECEIVED)
        );

        System.out.println("Sent " + amount + " " + senderAccount.getCurrency() + " (" + convertedAmount + " " + receiverAccount.getCurrency() + ") to " + receiverAccount.getIBAN() + (Utils.verifyIBAN(receiver) ? "" : " [" + receiver + "]"));

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
