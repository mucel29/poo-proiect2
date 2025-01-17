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
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.system.user.plan.ServicePlan;

public class CashWithdrawCommand extends Command.Base{

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

        if (!targetCard.isActive()) {
            throw new OperationException(
                    "Card is frozen",
                    "Frozen card: " + targetCard.getCardNumber(),
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        double convertedAmount = amount * BankingSystem.getExchangeProvider().getRate(
                "RON", targetAccount.getCurrency()
        );

        if (convertedAmount > targetAccount.getFunds()) {
            throw new OperationException(
                    "Insufficient funds",
                    "Account "
                            + targetAccount.getAccountIBAN()
                            + " has insufficient funds",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        if (targetAccount.getFunds() - convertedAmount < targetAccount.getMinBalance()) {
            throw new OperationException(
                    "Cannot perform payment due to a minimum balance being set",
                    "Account "
                            + targetAccount.getAccountIBAN()
                            + " will go under the minimum balance",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        targetAccount.setFunds(targetAccount.getFunds() - convertedAmount);
        targetUser.getServicePlan().applyFee(targetAccount, amount);

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
