package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.utils.Utils;

public class CreateCardCommand extends Command.Base {

    private Card.Type cardType;
    private final String IBAN;
    private final String email;

    public CreateCardCommand(Card.Type cardType, String IBAN, String email) {
        super(cardType.command());
        this.cardType = cardType;
        this.IBAN = IBAN;
        this.email = email;
    }

    // Package private constructor
    CreateCardCommand(Card.Type cardType, String IBAN, String email, int timestamp) {
        super(cardType.command());
        this.cardType = cardType;
        this.IBAN = IBAN;
        this.email = email;
        this.timestamp = timestamp;
    }

    @Override
    public void execute() throws UserNotFoundException, OwnershipException {
        User targetUser = BankingSystem.getUserByEmail(email);
        if (!BankingSystem.getUserByIBAN(IBAN).equals(targetUser)) {
            throw new OwnershipException("Account " + IBAN + " does not belong to " + email);
        }

        Account targetAccount = targetUser.getAccount(IBAN);
        Card newCard = new Card(targetAccount, cardType, Utils.generateCardNumber());

        targetAccount.getTransactions().add(
                new Transaction.CardOperation("New card created", timestamp)
                        .setCard(newCard.getCardNumber())
                        .setCardHolder(targetUser.getEmail())
                        .setAccount(targetAccount.getIBAN())
        );

        targetAccount.getCards().add(newCard);
    }

    public static CreateCardCommand fromNode(final JsonNode node, Card.Type cardType) throws BankingInputException {

        String email = node.get("email").asText();
        String IBAN = node.get("account").asText();

        if (email.isEmpty() || IBAN.isEmpty()) {
            throw new BankingInputException("Missing email / account for CreateCard\n" + node.toPrettyString());
        }


        return new CreateCardCommand(cardType, IBAN, email);
    }

}