package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
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
import org.poo.utils.Utils;

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
            targetCard
                    .getAccount()
                    .getTransactions()
                    .add(new Transaction.Base(
                            "The card is frozen",
                            timestamp
                    ));
            throw new OperationException("Card " + cardNumber + " is frozen");
        }

//        if (targetAccount.getFunds() < targetAccount.getMinBalance()) {
//            // Freeze card
//            targetCard.setStatus(false);
//            throw new OperationException("Balance under minimum");
//        }

        double deducted = amount * Exchange.getRate(currency, targetAccount.getCurrency());

        if (targetAccount.getFunds() < deducted) {
            targetAccount.getTransactions().add(
                    new Transaction.Base("Insufficient funds", timestamp)
            );
            throw new OperationException("Not enough balance: " + targetAccount.getFunds() + " (wanted to pay " + deducted + " " + targetAccount.getCurrency() + ") [" + amount + " " + currency + "]");
        }

        targetAccount.setFunds(targetAccount.getFunds() - deducted);
        System.out.println("Paid " + amount + " " + currency + " (" + deducted + " " + targetAccount.getCurrency() + ") to " + commerciant);

        targetAccount.getTransactions().add(
                new Transaction.Payment("Card payment", timestamp)
                        .setCommerciant(commerciant)
                        .setAmount(deducted)
        );

        // Generate a new one time card
        if (targetCard.getCardType() == Card.Type.ONE_TIME) {
            new DeleteCardCommand(
                    targetCard.getCardNumber(),
                    targetUser.getEmail(),
                    timestamp
            ).execute();
            new CreateCardCommand(
                    Card.Type.ONE_TIME,
                    targetAccount.getIBAN(),
                    targetUser.getEmail(),
                    timestamp
            ).execute();
        }

    }

    public static PayOnlineCommand fromNode(final JsonNode node) throws BankingInputException {
        double amount = IOUtils.readDoubleChecked(node, "amount");

        String cardNumber = IOUtils.readStringChecked(node, "cardNumber");
        String email = IOUtils.readStringChecked(node, "email");
        String description = IOUtils.readStringChecked(node, "description");
        String commerciant = IOUtils.readStringChecked(node, "commerciant");

        String currency = Exchange.verifyCurrency(
                IOUtils.readStringChecked(node, "currency")
        );

        return new PayOnlineCommand(email, cardNumber, amount, currency, description, commerciant);
    }

}
