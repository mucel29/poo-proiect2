package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Card;
import org.poo.system.user.User;

public class DeleteCardCommand extends Command.Base {

    private final String cardNumber;
    private final String email;

    public DeleteCardCommand(
            final String cardNumber,
            final String email
    ) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
        this.email = email;
    }

    // Package private constructor
    DeleteCardCommand(
            final String cardNumber,
            final String email,
            final int timestamp
    ) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
        this.email = email;
        this.timestamp = timestamp;
    }


    /**
     * {@inheritDoc}
     * @throws OwnershipException if the given card is not owned by the user
     * or no user owns the given card
     */
    @Override
    public void execute() throws OwnershipException {
        // Retrieve the user from the storage provider
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        // Retrieve the card from the storage provider
        Card targetCard = BankingSystem.getStorageProvider().getCard(cardNumber);

        // Check if the given user matches the card owner
        // NOTE: this could've been checked more easily
        // by retrieving the target card
        // and checking the owner's email with the provided email
        // but the storage provider also check if the user / account is registered
        if (!targetCard.getAccount().getOwner().equals(targetUser)) {
            throw new OwnershipException("Card " + cardNumber + " is not owned by " + email);
        }

        // Generate destroy transaction on the associated account
        targetCard.getAccount().getTransactions().add(
                new Transaction.CardOperation("The card has been destroyed", timestamp)
                        .setCardHolder(targetCard
                                .getAccount()
                                .getOwner()
                                .getEmail()
                        ).setCard(targetCard.getCardNumber())
                        .setAccount(targetCard
                                .getAccount()
                                .getAccountIBAN()
                        )
        );

        // Remove the card from storage
        BankingSystem.getStorageProvider().removeCard(targetCard);
        BankingSystem.log("Deleted card: " + cardNumber);
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String cardNumber = IOUtils.readStringChecked(node, "cardNumber");
        String email = IOUtils.readStringChecked(node, "email");

        return new DeleteCardCommand(cardNumber, email);
    }

}
