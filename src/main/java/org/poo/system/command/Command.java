package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

public interface Command {

    void execute();

    enum Type {
        ADD_ACCOUNT("addAccount"),
        ADD_CARD("createCard"),
        ADD_ONE_TIME_CARD("createCard"),
        ADD_FUNDS("addFunds"),
        PRINT_USERS("printUsers");

        private final String label;

        public static List<String> possibleValues() {
            return Arrays.stream(Command.Type.values()).map((command -> command.label)).toList();
        }

        public static Type fromString(String label) throws BankingInputException {
            try {
                return Arrays.stream(Command.Type.values()).filter(command -> command.label.equals(label)).toList().getFirst();
            } catch (NoSuchElementException e) {
                throw new BankingInputException("Unknown command: " + label);
            }
        }

        Type(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    static Command read(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("Command node is not an object");
        }

        Command.Type type = Type.fromString(node.get("command").asText());

        if (node.get("timestamp") == null) {
            throw new BankingInputException("Command has no command field or timestamp field");
        }

        Command.Base command = switch (type) {
            case ADD_ACCOUNT -> Command.AddAccount.fromNode(node);
            case ADD_CARD -> Command.CreateCard.fromNode(node, Card.Type.CLASSIC);
            case ADD_ONE_TIME_CARD -> Command.CreateCard.fromNode(node, Card.Type.ONE_TIME);
            case ADD_FUNDS -> Command.AddFunds.fromNode(node);
            case PRINT_USERS -> new PrintUsers();
        };

        command.timestamp = node.get("timestamp").asInt();
        return command;
    }

    static List<Command> readArray(final JsonNode node) throws BankingInputException {
        if (!node.isArray()) {
            throw new BankingInputException("Commands list is not an array");
        }

        ArrayNode commands = (ArrayNode) node;
        List<Command> commandsList = new ArrayList<>();

        for (JsonNode commandNode : commands) {
            try {
                commandsList.add(read(commandNode));
            } catch (BankingInputException e) {
                // If the command could not be read, continue reading the other commands, it's not a critical failing point
                e.printStackTrace();
            }
        }

        return commandsList;
    }

    @Getter
    abstract class Base implements Command {

        protected Type command;
        @Setter
        protected int timestamp;

        public Base(Type command) {
            this.command = command;
        }

    }

    class AddAccount extends Base {

        private String email;
        private BankingSystem.Currency currency;
        private Account.Type accountType;
        private double interest;

        public AddAccount(String email, BankingSystem.Currency currency, Account.Type accountType, double interest) {
            super(Command.Type.ADD_ACCOUNT);
            this.email = email;
            this.currency = currency;
            this.accountType = accountType;
            this.interest = interest;
        }

        @Override
        public void execute() throws UserNotFoundException {

            // Get user by using the email
            User targetUser = BankingSystem.getUserByEmail(this.email);
            if (targetUser == null) {
                return; // ??? output
            }

            // We know the currency is supported, otherwise there would've been an Exception already

            Account newAccount = new Account(Utils.generateIBAN(), this.currency, this.accountType);
            if (this.accountType == Account.Type.SAVINGS) {
                newAccount.setInterest(this.interest);
            }

            // Add the new account into the map and to the user
            BankingSystem.getInstance().getAccountMap().put(newAccount.getIBAN(), targetUser);
            targetUser.getAccounts().add(newAccount);
        }

        public static AddAccount fromNode(JsonNode node) throws BankingInputException {

            String email = node.get("email").asText();
            BankingSystem.Currency currency = BankingSystem.Currency.fromString(node.get("currency").asText());

            // This will throw an exception if it's empty anyway
            Account.Type accountType = Account.Type.fromString(node.get("accountType").asText());

            if (email.isEmpty()) {
                throw new BankingInputException("Missing email for AddAccount\n" + node.toPrettyString());
            }

            double interest = 0;
            if (accountType == Account.Type.SAVINGS) {
                if (node.get("interest") == null) {
                    throw new BankingInputException("Missing interest for savings account\n" + node.toPrettyString());
                }
                interest = node.get("interestRate").asDouble();
            }

            return new AddAccount(email, currency, accountType, interest);
        }

    }

    class CreateCard extends Base {

        private Card.Type cardType;
        private String IBAN;
        private String email;

        public CreateCard(Card.Type cardType, String IBAN, String email) {
            super(cardType.command());
            this.cardType = cardType;
            this.IBAN = IBAN;
            this.email = email;
        }

        @Override
        public void execute() throws UserNotFoundException, OwnershipException {
            User targetUser = BankingSystem.getUserByEmail(this.email);
            if (!BankingSystem.getUserByIBAN(this.IBAN).equals(targetUser)) {
                throw new OwnershipException("Account " + this.IBAN + " does not belong to " + this.email);
            }

            Account targetAccount = targetUser.getAccount(this.IBAN);
            targetAccount.getCards().add(new Card(targetAccount, this.cardType, Utils.generateCardNumber()));
        }

        public static CreateCard fromNode(final JsonNode node, Card.Type cardType) throws BankingInputException {

            String email = node.get("email").asText();
            String IBAN = node.get("account").asText();

            if (email.isEmpty() || IBAN.isEmpty()) {
                throw new BankingInputException("Missing email / account for CreateCard\n" + node.toPrettyString());
            }


            return new CreateCard(cardType, IBAN, email);
        }

    }

    class AddFunds extends Base {

        private String IBAN;
        private double amount;

        public AddFunds(String IBAN, double amount) {
            super(Type.ADD_FUNDS);
            this.IBAN = IBAN;
            this.amount = amount;
        }

        @Override
        public void execute() throws UserNotFoundException, OwnershipException {
            Account targetAccount = BankingSystem.getAccount(this.IBAN);
            targetAccount.setFunds(targetAccount.getFunds() + this.amount);
        }

        public static AddFunds fromNode(final JsonNode node) throws BankingInputException {
            String IBAN = node.get("account").asText();
            double amount = node.get("amount").asDouble(-1);

            if (IBAN.isEmpty() || amount < 0) {
                throw new BankingInputException("Missing account / amount for AddFunds\n" + node.toPrettyString());
            }

            return new AddFunds(IBAN, amount);
        }

    }


    class PrintUsers extends Base {

        public PrintUsers() {
            super(Type.PRINT_USERS);
        }

        @Override
        public void execute() {
            ObjectNode root = StateWriter.getMapper().createObjectNode();

            root.put("command", command.toString());
            root.put("timestamp", timestamp);

            ArrayNode output = root.putArray("output");
            for (User u : BankingSystem.getInstance().getUsers()){
                output.add(u.toNode());
            }

            StateWriter.write(root);
        }

    }

}