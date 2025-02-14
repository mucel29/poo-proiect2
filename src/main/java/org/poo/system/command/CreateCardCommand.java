package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Setter;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.utils.Utils;

@Setter
public class CreateCardCommand extends Command.Base {

    private Card.Type cardType;
    private String account;
    private String email;

    public CreateCardCommand(
            final Card.Type cardType,
            final String account,
            final String email
    ) {
        super(cardType.command());
        this.cardType = cardType;
        this.account = account;
        this.email = email;
    }

    // Package private constructor
    CreateCardCommand(
            final Card.Type cardType,
            final String account,
            final String email,
            final int timestamp
    ) {
        super(cardType.command());
        this.cardType = cardType;
        this.account = account;
        this.email = email;
        this.timestamp = timestamp;
    }


    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if no user exists with the given email
     * @throws OwnershipException if the given account is not authorized
     */
    @Override
    public void execute() throws UserNotFoundException, OwnershipException {

        // Retrieve the user from the storage provider
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);

        // Check if the user can create a card
        if (targetAccount.isUnauthorized(targetUser)) {
            throw new OwnershipException("User " + email + " is not authorized for " + account);
        }

        // Create a new card
        Card newCard = new Card(targetAccount, targetUser, cardType, Utils.generateCardNumber());

        BankingSystem.log(
                "Created card: "
                        + newCard.getCardNumber()
                        + " ["
                        + cardType
                        + "] "
        );

        // Generate creation transaction on the target account
        targetAccount.getTransactions().add(
                new Transaction.CardOperation("New card created", timestamp)
                        .setCard(newCard.getCardNumber())
                        .setCardHolder(targetUser.getEmail())
                        .setAccount(targetAccount.getAccountIBAN())
        );

        // Register the new card
        BankingSystem.getStorageProvider().registerCard(newCard);
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(
            final JsonNode node,
            final Card.Type cardType
    ) throws InputException {
        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");

        return new CreateCardCommand(cardType, account, email);
    }

}
