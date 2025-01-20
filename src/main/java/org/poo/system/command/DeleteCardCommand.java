package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Card;
import org.poo.system.user.User;

public class DeleteCardCommand extends Command.Base {

    private final String cardNumber;
    private final String email;
    private final boolean ignoreBalance;

    public DeleteCardCommand(
            final String cardNumber,
            final String email
    ) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
        this.email = email;
        this.ignoreBalance = false;
    }

    // Package private constructor
    DeleteCardCommand(
            final String cardNumber,
            final String email,
            final int timestamp,
            final boolean ignoreBalance
    ) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
        this.email = email;
        this.timestamp = timestamp;
        this.ignoreBalance = ignoreBalance;
    }


    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given email
     * @throws OwnershipException if the given card is not owned by the user
     * or no user owns the given card
     */
    @Override
    public void execute() throws UserNotFoundException, OwnershipException {
        // Retrieve the user and card from the storage provider
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        Card targetCard = BankingSystem.getStorageProvider().getCard(cardNumber);

        // If the balance shouldn't be ignored and the connected account still has funds,
        // Cancel the deletion
        if (!ignoreBalance
                && targetCard.getAccount().getFunds().total() > 0.0) {
            return;
        }

        // Delete the card
        targetCard.getAccount().authorizeCardDeletion(targetUser, targetCard);

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
