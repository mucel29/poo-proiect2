package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Card;

public class DeleteCardCommand extends Command.Base {

    private final String cardNumber;
    private final String email;

    public DeleteCardCommand(String cardNumber, String email) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
        this.email = email;
    }

    // Package private constructor
    DeleteCardCommand(String cardNumber, String email, int timestamp) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
        this.email = email;
        this.timestamp = timestamp;
    }


    @Override
    public void execute() throws OwnershipException {
        Card targetCard = BankingSystem.getCard(cardNumber);
        if (!targetCard.getAccount().getOwner().getEmail().equals(email)) {
            throw new OwnershipException("Card " + cardNumber + " is not owned by " + email);
        }
        targetCard.getAccount().getTransactions().add(
                new Transaction.CardOperation("The card has been destroyed", timestamp)
                        .setCardHolder(targetCard
                                .getAccount()
                                .getOwner()
                                .getEmail()
                        ).setCard(targetCard.getCardNumber())
                        .setAccount(targetCard
                                .getAccount()
                                .getIBAN()
                        )
        );
        targetCard.getAccount().getCards().remove(targetCard);
        System.out.println("Deleted card: " + cardNumber);
    }

    public static DeleteCardCommand fromNode(final JsonNode node) throws BankingInputException {
        String cardNumber = IOUtils.readStringChecked(node, "cardNumber");
        String email = IOUtils.readStringChecked(node, "email");

        return new DeleteCardCommand(cardNumber, email);
    }

}
