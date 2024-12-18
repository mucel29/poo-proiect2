package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.exceptions.InputException;
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
    private final String commerciant;

    public PayOnlineCommand(
            final String email,
            final String cardNumber,
            final double amount,
            final String currency,
            final String description,
            final String commerciant
    ) {
        super(Command.Type.PAY_ONLINE);
        this.email = email;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.commerciant = commerciant;
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
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        Card targetCard;
        try {
            targetCard = BankingSystem.getStorageProvider().getCard(cardNumber);
        } catch (OwnershipException e) {
            super.output(output -> {
                output.put("description", "Card not found");
                output.put("timestamp", super.timestamp);
            });
            throw new OwnershipException(e.getMessage());
        }
        Account targetAccount = targetCard.getAccount();

        if (!targetUser.getAccounts().contains(targetAccount)) {
            throw new OwnershipException("Card " + cardNumber + " is not owned by " + email);
        }

        if (!targetCard.isStatus()) {
            throw new OperationException(
                    "The card is frozen",
                    "Card " + cardNumber + " is frozen",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        double deducted = amount * BankingSystem.getExchangeProvider().getRate(
                currency,
                targetAccount.getCurrency()
        );

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
                        + commerciant
        );

        targetAccount.getTransactions().add(
                new Transaction.Payment("Card payment", timestamp)
                        .setCommerciant(commerciant)
                        .setAmount(deducted)
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
