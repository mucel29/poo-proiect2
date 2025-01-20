package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;

public class CashWithdrawCommand extends Command.Base {

    private final String cardNumber;
    private final double amount;
    private final String email;
    private final String location;

    public CashWithdrawCommand(
            final String cardNumber,
            final double amount,
            final String email,
            final String location
    ) {
        super(Command.Type.CASH_WITHDRAWAL);
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.email = email;
        this.location = location;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given email
     * @throws OwnershipException if no user owns the given card,
     * or the user is unauthorized to perform the withdrawal
     * @throws OperationException if the card is frozen
     * or the connected account doesn't have enough funds
     */
    @Override
    public void execute()
            throws UserNotFoundException, OwnershipException, OperationException {

        // Retrieve user and card
        Card targetCard;
        User targetUser;

        try {
            targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
            targetCard = BankingSystem.getStorageProvider().getCard(cardNumber);
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException(
                    "User not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        } catch (OwnershipException e) {
            throw new OwnershipException(
                    "Card not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        }


        Account targetAccount = targetCard.getAccount();

        // Check if the user is authorized to perform the withdrawal
        if (targetAccount.isUnauthorized(targetUser)) {
            throw new OwnershipException(
                    "Card not found",
                    targetUser
                        + " doesn't have access to card "
                        + cardNumber,
                    new CommandDescriptionHandler(this)
            );
        }

        // Check if the card is frozen
        if (!targetCard.isActive()) {
            throw new OperationException(
                    "Card is frozen",
                    "Frozen card: " + targetCard.getCardNumber(),
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        Amount requestedAmount = new Amount(amount, "RON");

        // Convert the requested amount into the account's currency
        Amount targetAmount = requestedAmount.to(targetAccount.getCurrency());

        // Perform withdrawal
        try {
            targetAccount.authorizeSpending(targetUser, targetAmount);
            targetAccount.applyFee(targetAmount);
        } catch (OperationException e) {
            throw new OperationException(
                    "Insufficient funds",
                    "Account "
                            + targetAccount.getAccountIBAN()
                            + " has insufficient funds",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        // Emmit withdrawal transaction to the connected account
        targetAccount.getTransactions().add(
                new Transaction.CashWithdrawal("Cash withdrawal of " + amount, timestamp)
                        .setAmount(amount)
        );

    }

    /**
     * Deserializes the given node into a {@code Command.Base}  instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String cardNumber = IOUtils.readStringChecked(node, "cardNumber");
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String email = IOUtils.readStringChecked(node, "email");
        String location = IOUtils.readStringChecked(node, "location");

        return new CashWithdrawCommand(cardNumber, amount, email, location);
    }
}
