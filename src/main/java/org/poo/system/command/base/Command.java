package org.poo.system.command.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.command.*;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.user.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

public interface Command {

    void execute();

    enum Type {
        ADD_ACCOUNT("addAccount"),
        CREATE_CARD("createCard"),
        CREATE_ONE_TIME_CARD("createOneTimeCard"),
        ADD_FUNDS("addFunds"),
        DELETE_ACCOUNT("deleteAccount"),
        DELETE_CARD("deleteCard"),
        SET_MIN_BALANCE("setMinimumBalance"),
        CHECK_CARD_STATUS("checkCardStatus"),
        PAY_ONLINE("payOnline"),
        SEND_MONEY("sendMoney"),
        SET_ALIAS("setAlias"),
        SPLIT_PAYMENT("splitPayment"),
        ADD_INTEREST("addInterest"),
        CHANGE_INTEREST_RATE("changeInterestRate"),
        REPORT("report"),
        SPENDING_REPORT("spendingReport"),

        PRINT_TRANSACTIONS("printTransactions"),
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
            case ADD_ACCOUNT -> AddAccountCommand.fromNode(node);
            case CREATE_CARD -> CreateCardCommand.fromNode(node, Card.Type.CLASSIC);
            case CREATE_ONE_TIME_CARD -> CreateCardCommand.fromNode(node, Card.Type.ONE_TIME);
            case ADD_FUNDS -> AddFundsCommand.fromNode(node);
            case DELETE_ACCOUNT -> DeleteAccountCommand.fromNode(node);
            case DELETE_CARD -> DeleteCardCommand.fromNode(node);
            case SET_MIN_BALANCE -> MinBalanceCommand.fromNode(node);
            case PAY_ONLINE -> PayOnlineCommand.fromNode(node);
            case SEND_MONEY -> SendMoneyCommand.fromNode(node);
            case SET_ALIAS -> SetAliasCommand.fromNode(node);
            case CHECK_CARD_STATUS -> CheckCardCommand.fromNode(node);
            case ADD_INTEREST -> AddInterestCommand.fromNode(node);
            case CHANGE_INTEREST_RATE -> ChangeInterestCommand.fromNode(node);
            case SPLIT_PAYMENT -> SplitPayCommand.fromNode(node);

            case PRINT_TRANSACTIONS -> PrintTransactionsCommand.fromNode(node);
            case PRINT_USERS -> new PrintUsersCommand();
            default -> throw new BankingInputException("[" + BankingSystem.TEST_NUMBER + "] Unknown command: " + type);
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
        protected int timestamp;

        public Base(Type command) {
            this.command = command;
        }

        protected void output(Consumer<ObjectNode> consumer) {
            ObjectNode root = StateWriter.getMapper().createObjectNode();
            root.put("timestamp", timestamp);
            root.put("command", command.toString());
            consumer.accept(root.putObject("output"));
            StateWriter.write(root);
        }

        protected void outputArray(Consumer<ArrayNode> consumer) {
            ObjectNode root = StateWriter.getMapper().createObjectNode();
            root.put("timestamp", timestamp);
            root.put("command", command.toString());
            consumer.accept(root.putArray("output"));
            StateWriter.write(root);
        }

    }

}