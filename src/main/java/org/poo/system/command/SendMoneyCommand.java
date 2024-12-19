package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.AliasException;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.handlers.TransactionHandler;
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
     * {@inheritDoc}
     * @throws OwnershipException if no user owns the sender or receiver accounts
     * @throws OperationException
     * <ul>
     *      <li> if the sender is an alias instead of an account </li>
     *      <li> if the sender doesn't have enough funds </li>
     * </ul>
     */
    @Override
    public void execute() throws AliasException, OwnershipException, OperationException {
        // Check if the sender is a valid account and not an alias
        if (!Utils.verifyIBAN(sender)) {
            throw new AliasException("Invalid sender IBAN [can't use an user as alias]");
        }

        // Retrieve the sender account from the storage provider
        Account senderAccount = BankingSystem.getStorageProvider().getAccountByIban(sender);

        // Retrieve the receiver account from the storage provider
        Account receiverAccount = Utils.verifyIBAN(receiver)
                ? BankingSystem.getStorageProvider().getAccountByIban(receiver)
                : BankingSystem.getStorageProvider().getAccountByAlias(receiver);

        // Convert he amount to be transferred to the receiver
        double convertedAmount = amount * BankingSystem.getExchangeProvider().getRate(
                senderAccount.getCurrency(),
                receiverAccount.getCurrency()
        );

        // Check if the sender has enough funds
        if (senderAccount.getFunds() < amount) {
            throw new OperationException(
                    "Insufficient funds",
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
                            + "]",
                    new TransactionHandler(senderAccount, timestamp)
            );
        }

        // Create the transaction
        Transaction.Transfer transaction = new Transaction.Transfer(description, timestamp)
                .setSenderIBAN(senderAccount.getAccountIBAN())
                .setReceiverIBAN(receiverAccount.getAccountIBAN())
                .setAmount(amount)
                .setCurrency(senderAccount.getCurrency())
                .setTransferType(Transaction.TransferType.SENT);

        // Deduct the amount from the sender's account and emmit a copy of the transaction
        senderAccount.setFunds(senderAccount.getFunds() - amount);
        senderAccount.getTransactions().add(transaction.clone());

        // Add the amount to the receiver's account and emmit a `received` transaction
        receiverAccount.setFunds(receiverAccount.getFunds() + convertedAmount);
        receiverAccount.getTransactions().add(
                transaction
                        .setCurrency(receiverAccount.getCurrency())
                        .setAmount(convertedAmount)
                        .setTransferType(Transaction.TransferType.RECEIVED)
        );

        BankingSystem.log(
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
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        String receiver = IOUtils.readStringChecked(node, "receiver");
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String description = IOUtils.readStringChecked(node, "description");

        return new SendMoneyCommand(account, receiver, amount, description);
    }

}
