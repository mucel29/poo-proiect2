package org.poo.system.command.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.io.IOUtils;
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
import java.util.function.Function;
import java.util.function.Supplier;

public interface Command {

    void execute();

    enum Type {
        ADD_ACCOUNT(
                "addAccount",
                AddAccountCommand::fromNode
        ),
        CREATE_CARD(
                "createCard",
                node -> CreateCardCommand.fromNode(node, Card.Type.CLASSIC)
        ),
        CREATE_ONE_TIME_CARD(
                "createOneTimeCard",
                node -> CreateCardCommand.fromNode(node, Card.Type.ONE_TIME)
        ),
        ADD_FUNDS(
                "addFunds",
                AddFundsCommand::fromNode
        ),
        DELETE_ACCOUNT(
                "deleteAccount",
                DeleteAccountCommand::fromNode
        ),
        DELETE_CARD(
                "deleteCard",
                DeleteCardCommand::fromNode
        ),
        SET_MIN_BALANCE(
                "setMinimumBalance",
                MinBalanceCommand::fromNode
        ),
        CHECK_CARD_STATUS(
                "checkCardStatus",
                CheckCardCommand::fromNode
        ),
        PAY_ONLINE(
                "payOnline",
                PayOnlineCommand::fromNode
        ),
        SEND_MONEY(
                "sendMoney",
                SendMoneyCommand::fromNode
        ),
        SET_ALIAS(
                "setAlias",
                SetAliasCommand::fromNode
        ),
        SPLIT_PAYMENT(
                "splitPayment",
                SplitPayCommand::fromNode
        ),
        ADD_INTEREST(
                "addInterest",
                AddInterestCommand::fromNode
        ),
        CHANGE_INTEREST_RATE(
                "changeInterestRate",
                ChangeInterestCommand::fromNode
        ),
        REPORT(
                "report",
                ReportCommand::fromNode
        ),
        SPENDINGS_REPORT(
                "spendingsReport",
                SpendingReportCommand::fromNode
        ),

        PRINT_TRANSACTIONS(
                "printTransactions",
                PrintTransactionsCommand::fromNode
        ),
        PRINT_USERS(
                "printUsers",
                node -> new PrintUsersCommand()
        );

        private final String label;
        private final Function<JsonNode, Command.Base> commandSupplier;

        public static Type fromString(String label) throws BankingInputException {
            try {
                return Arrays.stream(Command.Type.values()).filter(command -> command.label.equals(label)).toList().getFirst();
            } catch (NoSuchElementException e) {
                throw new BankingInputException("Unknown command: " + label);
            }
        }

        Type(final String label, final Function<JsonNode, Command.Base> commandSupplier) {
            this.label = label;
            this.commandSupplier = commandSupplier;
        }

        @Override
        public String toString() {
            return label;
        }

        public Command.Base parse(final JsonNode node) {
            return commandSupplier.apply(node);
        }
    }

    static Command read(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("Command node is not an object");
        }

        Command.Type type = Command.Type.fromString(
                IOUtils.readStringChecked(node, "command")
        );

        int timestamp = IOUtils.readIntChecked(node, "timestamp");

        Command.Base command = type.parse(node);
        command.timestamp = timestamp;

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