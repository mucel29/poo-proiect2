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
     * Executes the command instance. May produce transactions or errors.
     */
    @Override
    public void execute() {

        Card targetCard;
        User targetUser;

        try {
            targetCard = BankingSystem.getStorageProvider().getCard(cardNumber);
            targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        } catch (OwnershipException e) {
            throw new OwnershipException(
                    "Card not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException(
                    "User not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        }

        // Check if the user and owner are the same???


        Account targetAccount = targetCard.getAccount();


        if (!targetAccount.isAuthorized(targetUser)) {
            throw new OwnershipException(
                    "Card not found",
                    targetUser
                        + " doesn't have access to card "
                        + cardNumber,
                    new CommandDescriptionHandler(this)
            );
        }


        if (!targetCard.isActive()) {
            throw new OperationException(
                    "Card is frozen",
                    "Frozen card: " + targetCard.getCardNumber(),
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        Amount requestedAmount = new Amount(amount, "RON");

        Amount targetAmount = requestedAmount.to(targetAccount.getCurrency());

//        targetAmount = targetUser.getServicePlan().applyFee(targetAmount);


//        if (targetAccount.getFunds().total() < targetAmount.total()) {
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

//        if (targetAccount.getFunds().sub(targetAmount).total() < targetAccount.getMinBalance()) {
//            throw new OperationException(
//                    "Cannot perform payment due to a minimum balance being set",
//                    "Account "
//                            + targetAccount.getAccountIBAN()
//                            + " will go under the minimum balance",
//                    new TransactionHandler(targetAccount, timestamp)
//            );
//        }

//        targetAccount.setFunds(targetAccount.getFunds().sub(targetAmount));

        targetAccount.getTransactions().add(
                new Transaction.CashWithdrawal("Cash withdrawal of " + amount, timestamp)
                        .setAmount(amount)
        );

        // Generate a new one time card
//        if (targetCard.getCardType() == Card.Type.ONE_TIME) {
//            try {
//                new DeleteCardCommand(
//                        targetCard.getCardNumber(),
//                        // change to account owner
//                        targetUser.getEmail(),
//                        timestamp,
//                        false
//                ).execute();
//            } catch (OwnershipException e) {
//                return;
//            }
//            new CreateCardCommand(
//                    Card.Type.ONE_TIME,
//                    targetAccount.getAccountIBAN(),
//                    targetUser.getEmail(),
//                    timestamp
//            ).execute();
//        }

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
