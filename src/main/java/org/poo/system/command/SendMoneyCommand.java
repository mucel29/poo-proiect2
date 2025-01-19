package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.AliasException;
import org.poo.system.exceptions.BankingException;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;
import org.poo.system.user.User;
import org.poo.utils.Utils;

public class SendMoneyCommand extends Command.Base {

    private final String sender;
    private final String receiver;
    private final String email;
    private final double amount;
    private final String description;

    public SendMoneyCommand(
            final String sender,
            final String receiver,
            final String email,
            final double amount,
            final String description
    ) {
        super(Type.SEND_MONEY);
        this.sender = sender;
        this.receiver = receiver;
        this.email = email;
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

        // Check if there's any receiver
        if (receiver.isEmpty()) {
            throw new UserNotFoundException(
                    "User not found",
                    "Receiver field is empty",
                    new CommandDescriptionHandler(this)
            );
        }

        // Check if the sender is a valid account and not an alias
        if (!Utils.verifyIBAN(sender)) {
            throw new AliasException("Invalid sender IBAN [can't use an user as alias]");
        }

        // Retrieve the sender account from the storage provider
        Account senderAccount = BankingSystem.getStorageProvider().getAccountByIban(sender);
        User senderUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        // Retrieve the receiver account from the storage provider
        Account receiverAccount;
        try {
            receiverAccount = Utils.verifyIBAN(receiver)
                    ? BankingSystem.getStorageProvider().getAccountByIban(receiver)
                    : BankingSystem.getStorageProvider().getAccountByAlias(receiver);
        } catch (BankingException e) {
            // Todo: get a commerciant
            throw new OwnershipException(
                    "User not found", // kinda dumb ngl
                    "Receiver not found: " + receiver,
                    new CommandDescriptionHandler(this)
            );
        }

        if (amount <= 0.0) {
            BankingSystem.log("Tried to pay 0.0 to " + receiver);
            return;
        }

        Amount senderAmount = new Amount(amount, senderAccount.getCurrency());

        // Convert the total to be transferred to the receiver
        Amount receiverAmount = senderAmount.to(receiverAccount.getCurrency());

        // might change to senderUser
//        senderAmount =  senderAccount.getOwner().getServicePlan().applyFee(senderAmount);


        // Check if the sender has enough funds
//        if (!senderAccount.canPay(senderAmount, true)) {
        try {
            BankingSystem.log(
                    "Authorizing sending: " + senderAmount.add(
                            senderAccount.getOwner().getServicePlan().getFee(senderAmount))

                    );
            senderAccount.authorizeSpending(senderUser, senderAmount.add(
                    senderAccount.getOwner().getServicePlan().getFee(senderAmount)
            ));
        } catch (OperationException e) {
            throw new OperationException(
                    "Insufficient funds",
                    "Not enough balance: "
                            + senderAccount.getFunds()
                            + " (wanted to send "
                            + senderAmount
                            + ") ["
                            + receiverAmount
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

        // Emmit a copy of the transaction
        senderAccount.getTransactions().add(transaction.clone());

        // Add the total to the receiver's account and emmit a `received` transaction
//        receiverAccount.setFunds(receiverAccount.getFunds().add(receiverAmount));
        receiverAccount.authorizeDeposit(receiverAccount.getOwner(), receiverAmount);
        receiverAccount.getTransactions().add(
                transaction
                        .setCurrency(receiverAccount.getCurrency())
                        .setAmount(receiverAmount.total())
                        .setTransferType(Transaction.TransferType.RECEIVED)
        );

        BankingSystem.log(
                "Sent "
                        + amount
                        + " "
                        + senderAccount.getCurrency()
                        + " ("
                        + receiverAmount
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
        String email = IOUtils.readStringChecked(node, "email");

        return new SendMoneyCommand(account, receiver, email, amount, description);
    }

}
