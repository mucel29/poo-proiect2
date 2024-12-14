package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.exchange.Exchange;
import org.poo.system.Transaction;
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
    private final String currency;
    private final String description;
    private final String commerciant;

    public PayOnlineCommand(String email, String cardNumber, double amount, String currency, String description, String commerciant) {
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
        Card targetCard;
        try {
            targetCard = BankingSystem.getCard(cardNumber);
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
            throw new OperationException("Card " + cardNumber + " is frozen");
        }

        if (targetAccount.getFunds() < targetAccount.getMinBalance()) {
            // Freeze card
            targetCard.setStatus(false);
            throw new OperationException("Balance under minimum");
        }

        double deducted = amount / Exchange.getRate(targetAccount.getCurrency(), currency);

        if (targetAccount.getFunds() < deducted) {
            targetUser.getTransactions().add(
                    new Transaction.Base("Insufficient funds", timestamp)
            );
            throw new OperationException("Not enough balance: " + targetAccount.getFunds() + " (wanted to pay " + deducted + " " + targetAccount.getCurrency() + ") [" + amount + " " + currency + "]");
        }

        BankingSystem.addCommerciantPayment(commerciant, deducted);
        targetAccount.setFunds(targetAccount.getFunds() - deducted);
        System.out.println("Paid " + amount + " " + currency + " (" + deducted + " " + targetAccount.getCurrency() + ") to " + commerciant);

        targetUser.getTransactions().add(
                new Transaction.Payment("Card payment", timestamp)
                        .setCommerciant(commerciant)
                        .setAmount(deducted)
        );

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

        String currency = Exchange.verifyCurrency(node.get("currency").asText());

        if (cardNumber.isEmpty() || amount < 0 || description.isEmpty() || commerciant.isEmpty()) {
            throw new BankingInputException("Missing arguments for PayOnline\n" + node.toPrettyString());
        }

        return new PayOnlineCommand(email, cardNumber, amount, currency, description, commerciant);
    }

}
