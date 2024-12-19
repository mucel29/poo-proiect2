package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.user.Account;
import org.poo.system.user.Card;

public class CheckCardCommand extends Command.Base {

    private static final double WARNING_THRESHOLD = 30.0;
    private final String cardNumber;

    public CheckCardCommand(final String cardNumber) {
        super(Type.CHECK_CARD_STATUS);
        this.cardNumber = cardNumber;
    }

    /**
     * {@inheritDoc}
     * @throws OwnershipException if the card does not belong to anyone
     */
    @Override
    public void execute() throws OwnershipException {

        // Retrieve the card from the storage provider, generate error on fail
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

        // Retrieve the associated account
        Account link = targetCard.getAccount();

        // Verify the account's balance
        if (link.getFunds() <= link.getMinBalance()) {
            // Freeze card
            targetCard.setActive(false);
            link.getTransactions().add(new Transaction.Base(
                    "You have reached the minimum amount of funds, the card will be frozen",
                    timestamp
            ));
        } else if (link.getFunds() - link.getMinBalance() <= WARNING_THRESHOLD) {
            // The account is nearing the minimum balance threshold
            // Issue a warning
            link.getTransactions().add(new Transaction.Base(
                    "Warning??????",
                    timestamp
            ));
        }

    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String cardNumber = IOUtils.readStringChecked(node, "cardNumber");

        return new CheckCardCommand(cardNumber);
    }

}
