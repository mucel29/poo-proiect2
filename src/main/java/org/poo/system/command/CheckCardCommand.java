package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;

public class CheckCardCommand extends Command.Base {

    private final double WARNING_THRESHOLD = 30.0;
    private final String cardNumber;

    public CheckCardCommand(String cardNumber) {
        super(Type.CHECK_CARD_STATUS);
        this.cardNumber = cardNumber;
    }

    @Override
    public void execute()
    {
        Card c;
        try {
            c = BankingSystem.getCard(cardNumber);
        } catch (OwnershipException e) {
            super.output((root) -> {
                root.put("description", "Card not found");
                root.put("timestamp", timestamp);
            });
            return;
        }
        Account link = c.getAccount();
        if (link.getFunds() <= link.getMinBalance()) {
            // Freeze card
            c.setStatus(false);
            link.getOwner().getTransactions().add(new Transaction.Base(
                    "You have reached the minimum amount of funds, the card will be frozen",
                    timestamp
            ));
        } else if (link.getFunds() - link.getMinBalance() <= WARNING_THRESHOLD) {
            link.getOwner().getTransactions().add(new Transaction.Base(
                    "Warning??????",
                    timestamp
            ));
        }

    }

    public static CheckCardCommand fromNode(final JsonNode node) throws BankingInputException {
        String cardNumber = node.get("cardNumber").asText();
        if (cardNumber.isEmpty()) {
            throw new BankingInputException("Missing argument for CheckCard\n" + node.toPrettyString());
        }

        return new CheckCardCommand(cardNumber);
    }

}
