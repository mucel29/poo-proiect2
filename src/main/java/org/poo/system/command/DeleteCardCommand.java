package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;

public class DeleteCardCommand extends Command.Base {

    private final String cardNumber;

    public DeleteCardCommand(String cardNumber) {
        super(Type.DELETE_CARD);
        this.cardNumber = cardNumber;
    }

    @Override
    public void execute() throws OwnershipException {
        Card targetCard = BankingSystem.getCard(cardNumber);
        targetCard.getAccount().getOwner().getTransactions().add(
                new Transaction("The card has been destroyed", timestamp)
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
        String cardNumber = node.get("cardNumber").asText();
        if (cardNumber.isEmpty()) {
            throw new BankingInputException("Missing cardNumber for DeleteCard\n" + node.toPrettyString());
        }

        return new DeleteCardCommand(cardNumber);
    }

}
