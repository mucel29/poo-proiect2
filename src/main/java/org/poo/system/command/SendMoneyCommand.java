package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
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

    public SendMoneyCommand(
            final String sender,
            final String receiver,
            final double amount,
            final String description
    ) {
        super(Type.SEND_MONEY);
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.description = description;
    }


    /**
     * @throws OwnershipException if no user owns the sender or receiver accounts
     * @throws OperationException - if the sender is an alias instead of an account
     * - if the sender doesn't have enough funds
     */
    @Override
    public void execute() throws OwnershipException, OperationException {
        if (!Utils.verifyIBAN(sender)) {
            throw new OperationException("Invalid sender IBAN [can't use an user as alias]");
        }

        Account senderAccount = BankingSystem.getAccount(sender);
        Account receiverAccount = Utils.verifyIBAN(receiver)
                ? BankingSystem.getAccount(receiver)
                : BankingSystem.getByAlias(receiver);

        double convertedAmount = amount * Exchange.getRate(
                senderAccount.getCurrency(),
                receiverAccount.getCurrency()
        );

        if (senderAccount.getFunds() < amount) {
            senderAccount
                    .getTransactions()
                    .add(new Transaction.Base(
                            "Insufficient funds",
                            timestamp
                    ));
            throw new OperationException(
                    "Not enough balance: "
                            + senderAccount.getFunds()
                            + " (wanted to send "
                            + amount
                            + " "
                            + senderAccount.getCurrency()
                            + ") ["
                            + convertedAmount
                            + " "
                            + receiverAccount.getCurrency()
                            + "]"
            );
        }

        Transaction.Transfer transaction = new Transaction.Transfer(description, timestamp)
                .setSenderIBAN(senderAccount.getAccountIBAN())
                .setReceiverIBAN(receiverAccount.getAccountIBAN())
                .setAmount(amount)
                .setCurrency(senderAccount.getCurrency())
                .setTransferType(Transaction.TransferType.SENT);

        senderAccount.setFunds(senderAccount.getFunds() - amount);
        senderAccount.getTransactions().add(transaction.clone());

        receiverAccount.setFunds(receiverAccount.getFunds() + convertedAmount);
        receiverAccount.getTransactions().add(
                transaction
                        .setCurrency(receiverAccount.getCurrency())
                        .setAmount(convertedAmount)
                        .setTransferType(Transaction.TransferType.RECEIVED)
        );

        System.out.println(
                "Sent "
                        + amount
                        + " "
                        + senderAccount.getCurrency()
                        + " ("
                        + convertedAmount
                        + " "
                        + receiverAccount.getCurrency()
                        + ") to "
                        + receiverAccount.getAccountIBAN()
                        + (Utils.verifyIBAN(receiver)
                        ? ""
                        : " ["
                        + receiver
                        + "]")
        );

    }

    /**
     * Deserializes the given node into a `Command.Base` instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws BankingInputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws BankingInputException {
        String account = IOUtils.readStringChecked(node, "account");
        String receiver = IOUtils.readStringChecked(node, "receiver");
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String description = IOUtils.readStringChecked(node, "description");

        return new SendMoneyCommand(account, receiver, amount, description);
    }

}
