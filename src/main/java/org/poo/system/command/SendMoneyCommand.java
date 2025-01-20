package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.commerce.Commerciant;
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


    private void payCommerciant(
            final User senderUser,
            final Account senderAccount,
            final Commerciant commerciant,
            final Amount senderAmount,
            final Transaction transaction
    ) throws OperationException {
        try {
            // Pay the commerciant, apply fees, apply cashback
            senderAccount.authorizeSpending(senderUser, senderAmount);
            senderAccount.applyFee(senderAmount);
            senderAccount.applyCashBack(senderUser, commerciant, senderAmount);

            // Emmit the transaction
            senderAccount.getTransactions().add(transaction);

            BankingSystem.log(
                    "Paid "
                            + amount
                            + " ("
                            + senderAmount
                            + ") to "
                            + commerciant.getName()
                            + " [transfer]"
            );

        } catch (OperationException o) {
            throw new OperationException(
                    "Insufficient funds",
                    "Not enough balance: "
                            + senderAccount.getFunds()
                            + " (wanted to pay "
                            + senderAmount
                            + ")",
                    new TransactionHandler(senderAccount, timestamp)
            );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws AliasException if the sender is an alias instead of an account
     * @throws OwnershipException if no user owns the sender
     * or receiver / commerciant accounts
     * @throws OperationException
     * <ul>
     *      <li> if the sender is unauthorized to perform the transfer </li>
     *      <li> if the sender doesn't have enough funds </li>
     * </ul>
     */
    @Override
    public void execute() throws AliasException, OwnershipException, OperationException {

        // Check if the sender is a valid account and not an alias
        if (!Utils.verifyIBAN(sender)) {
            throw new AliasException("Invalid sender IBAN [can't use an user as alias]");
        }

        // Retrieve the sender account and user from the storage provider
        Account senderAccount = BankingSystem.getStorageProvider().getAccountByIban(sender);
        User senderUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        Amount senderAmount = new Amount(amount, senderAccount.getCurrency());

        // Create the transaction
        Transaction.Transfer transaction = new Transaction.Transfer(description, timestamp)
                .setSenderIBAN(senderAccount.getAccountIBAN())
                .setReceiverIBAN(receiver)
                .setAmount(amount)
                .setCurrency(senderAccount.getCurrency())
                .setTransferType(Transaction.TransferType.SENT);

        // Retrieve the receiver account from the storage provider
        Account receiverAccount;
        try {
            receiverAccount = Utils.verifyIBAN(receiver)
                    ? BankingSystem.getStorageProvider().getAccountByIban(receiver)
                    : BankingSystem.getStorageProvider().getAccountByAlias(receiver);
        } catch (BankingException e) { // Catches OwnershipException or AliasException

            // No registered user account found using the given IBAN
            try {
                // Trying to find a commerciant with the given IBAN
                Commerciant commerciant = BankingSystem
                        .getStorageProvider()
                        .getCommerciantByIban(receiver);

                // Try to pay the commerciant
                payCommerciant(
                        senderUser,
                        senderAccount,
                        commerciant,
                        senderAmount,
                        transaction
                );

                return;
            } catch (UserNotFoundException unf) {
                // No account or commerciant found
                throw new OwnershipException(
                        "User not found",
                        "Receiver not found: " + receiver,
                        new CommandDescriptionHandler(this)
                );
            }
        }

        // Receiver was indeed a user account

        // Convert the total to be transferred to the receiver
        Amount receiverAmount = senderAmount.to(receiverAccount.getCurrency());

        try {
            Amount senderFee =
                    senderAccount
                            .getOwner()
                            .getServicePlan()
                            .getFee(senderAccount, senderAmount);

            // Authorize the transfer
            senderAccount.authorizeSpending(
                    senderUser,
                    senderAmount.add(senderFee)
            );
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

        // Emmit a `sent` transaction
        senderAccount.getTransactions().add(transaction.clone());

        // Deposit the funds to the receiver
        receiverAccount.authorizeDeposit(receiverAccount.getOwner(), receiverAmount);

        // Emmit a `received` transaction
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
