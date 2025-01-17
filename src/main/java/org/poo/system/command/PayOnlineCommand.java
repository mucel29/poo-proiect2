package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.exchange.ExchangeException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;

public class PayOnlineCommand extends Command.Base {

    private final String email;
    private final String cardNumber;
    private final double amount;
    private final String currency;
    private final String description;
    private final String commerciantName;

    public PayOnlineCommand(
            final String email,
            final String cardNumber,
            final double amount,
            final String currency,
            final String description,
            final String commerciantName
    ) {
        super(Command.Type.PAY_ONLINE);
        this.email = email;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.commerciantName = commerciantName;
    }

    /**
     * {@inheritDoc}
     * @throws UserNotFoundException if the given user does not exist
     * @throws OwnershipException if the given account is not owned by the given user
     * @throws ExchangeException if no exchange from the account's currency
     * to the given currency exists
     */
    @Override
    public void execute() throws UserNotFoundException, OwnershipException, ExchangeException {
        // Retrieve the user from the storage provider
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        // Retrieve the card from the storage provider
        Card targetCard;
        try {
            targetCard = BankingSystem.getStorageProvider().getCard(cardNumber);
        } catch (OwnershipException e) {
            throw new OwnershipException(
                    "Card not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        }
        Account targetAccount = targetCard.getAccount();

        // Check the ownership
        if (!targetUser.getAccounts().contains(targetAccount)) {
            throw new OwnershipException("Card " + cardNumber + " is not owned by " + email);
        }

        // Check if the card is frozen
        if (!targetCard.isActive()) {
            throw new OperationException(
                    "The card is frozen",
                    "Card " + cardNumber + " is frozen",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        if (amount <= 0.0) {
            BankingSystem.log("Tried to pay 0.0 to " + commerciantName);
            return;
        }

        // Convert from requested currency to the account's currency
        double deducted = amount * BankingSystem.getExchangeProvider().getRate(
                currency,
                targetAccount.getCurrency()
        );

        // Check if the account has enough funds for the payment
        if (targetAccount.getFunds() < deducted) {
            throw new OperationException(
                    "Insufficient funds",
                    "Not enough balance: "
                            + targetAccount.getFunds()
                            + " (wanted to pay "
                            + deducted
                            + " "
                            + targetAccount.getCurrency()
                            + ") ["
                            + amount
                            + " "
                            + currency
                            + "]",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        // Deduct from the account's funds
        targetAccount.setFunds(targetAccount.getFunds() - deducted);
        BankingSystem.log(
                "Paid "
                        + amount
                        + " "
                        + currency
                        + " ("
                        + deducted
                        + " "
                        + targetAccount.getCurrency()
                        + ") to "
                        + commerciantName
        );

        // Emmit payment transaction
        targetAccount.getTransactions().add(
                new Transaction.Payment("Card payment", timestamp)
                        .setCommerciant(commerciantName)
                        .setAmount(deducted)
        );

        double ronAmount = amount * BankingSystem.getExchangeProvider().getRate(currency, "RON");

        Commerciant commerciant = BankingSystem
                .getStorageProvider()
                .getCommerciantByName(commerciantName);

        targetAccount.getOwner().getServicePlan().applyFee(targetAccount, ronAmount);

        commerciant.getStrategy().apply(
                targetAccount,
                ronAmount
        );

        // Generate a new one time card
        if (targetCard.getCardType() == Card.Type.ONE_TIME) {
            new DeleteCardCommand(
                    targetCard.getCardNumber(),
                    targetUser.getEmail(),
                    timestamp
            ).execute();
            new CreateCardCommand(
                    Card.Type.ONE_TIME,
                    targetAccount.getAccountIBAN(),
                    targetUser.getEmail(),
                    timestamp
            ).execute();
        }

    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        double amount = IOUtils.readDoubleChecked(node, "amount");

        String cardNumber = IOUtils.readStringChecked(node, "cardNumber");
        String email = IOUtils.readStringChecked(node, "email");
        String description = IOUtils.readStringChecked(node, "description");
        String commerciant = IOUtils.readStringChecked(node, "commerciant");

        String currency = BankingSystem.getExchangeProvider().verifyCurrency(
                IOUtils.readStringChecked(node, "currency")
        );

        return new PayOnlineCommand(email, cardNumber, amount, currency, description, commerciant);
    }

}
