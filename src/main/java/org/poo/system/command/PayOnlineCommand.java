package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.Exchange;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.ExchangeException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;

public class PayOnlineCommand extends Command.Base {

    private final String email;
    private final String cardNumber;
    private final double amount;
    private final Exchange.Currency currency;
    private final String description;
    private final String commerciant;

    public PayOnlineCommand(String email, String cardNumber, double amount, Exchange.Currency currency, String description, String commerciant) {
        super(Command.Type.PAY_ONLINE);
        this.email = email;
        this.cardNumber = cardNumber;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.commerciant = commerciant;
    }

    @Override
    public void execute() throws OwnershipException, ExchangeException {
        User targetUser = BankingSystem.getUserByEmail(email);
        Card targetCard = BankingSystem.getCard(cardNumber);
        Account targetAccount = targetCard.getAccount();

        if (!targetUser.getAccounts().contains(targetAccount)) {
            throw new OwnershipException("Card " + cardNumber + " is not owned by " + email);
        }

        if (!targetCard.isStatus()) {
            throw new OperationException("Card " + cardNumber + " is frozen");
        }

        if (targetAccount.getFunds() < targetAccount.getMinBalance()) {
            // Freeze card
            targetCard.setStatus(false);
            throw new OperationException("Balance under minimum");
        }

        double deducted = amount * BankingSystem.getExchangeRate(targetAccount.getCurrency(), currency);

        if (targetAccount.getFunds() < deducted) {
            throw new OperationException("Not enough balance");
        }

        BankingSystem.addCommerciantPayment(commerciant, deducted);
        targetAccount.setFunds(targetAccount.getFunds() - deducted);

        // Freeze the one time card
        if (targetCard.getCardType() == Card.Type.ONE_TIME) {
            targetCard.setStatus(false);
        }

    }

    public static PayOnlineCommand fromNode(final JsonNode node) throws BankingInputException {
        double amount = node.get("amount").asDouble(-1);
        String cardNumber = node.get("cardNumber").asText();
        String email = node.get("email").asText();
        String description = node.get("description").asText();
        String commerciant = node.get("commerciant").asText();

        Exchange.Currency currency = Exchange.Currency.fromString(node.get("currency").asText());

        if (cardNumber.isEmpty() || amount < 0 || description.isEmpty() || commerciant.isEmpty()) {
            throw new BankingInputException("Missing arguments for PayOnline\n" + node.toPrettyString());
        }

        return new PayOnlineCommand(email, cardNumber, amount, currency, description, commerciant);
    }

}
