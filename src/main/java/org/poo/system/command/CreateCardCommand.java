package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Setter;
import org.poo.io.IOUtils;
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

@Setter
public class CreateCardCommand extends Command.Base {

    private Card.Type cardType;
    private String account;
    private String email;

    public CreateCardCommand(Card.Type cardType, String account, String email) {
        super(cardType.command());
        this.cardType = cardType;
        this.account = account;
        this.email = email;
    }

    // Package private constructor
    CreateCardCommand(Card.Type cardType, String account, String email, int timestamp) {
        super(cardType.command());
        this.cardType = cardType;
        this.account = account;
        this.email = email;
        this.timestamp = timestamp;
    }

    public CreateCardCommand()
    {
        super(Command.Type.CREATE_CARD);
    }

    @Override
    public void execute() throws UserNotFoundException, OwnershipException {
        super.command = this.cardType.command();
        User targetUser = BankingSystem.getUserByEmail(email);
        if (!BankingSystem.getUserByIBAN(account).equals(targetUser)) {
            throw new OwnershipException("Account " + account + " does not belong to " + email);
        }

        Account targetAccount = targetUser.getAccount(account);
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
        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");

        return new CreateCardCommand(cardType, account, email);
    }

}