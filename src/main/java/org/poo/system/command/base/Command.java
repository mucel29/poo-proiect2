package org.poo.system.command.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.io.IOUtils;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.command.AddAccountCommand;
import org.poo.system.command.AddFundsCommand;
import org.poo.system.command.AddInterestCommand;
import org.poo.system.command.ChangeInterestCommand;
import org.poo.system.command.CheckCardCommand;
import org.poo.system.command.CreateCardCommand;
import org.poo.system.command.DeleteAccountCommand;
import org.poo.system.command.DeleteCardCommand;
import org.poo.system.command.MinBalanceCommand;
import org.poo.system.command.PayOnlineCommand;
import org.poo.system.command.PrintTransactionsCommand;
import org.poo.system.command.PrintUsersCommand;
import org.poo.system.command.ReportCommand;
import org.poo.system.command.SendMoneyCommand;
import org.poo.system.command.SetAliasCommand;
import org.poo.system.command.SpendingReportCommand;
import org.poo.system.command.SplitPayCommand;
import org.poo.system.exceptions.InputException;
import org.poo.system.user.Card;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Command {

    /**
     * Executes the command instance. May produce transactions or errors.
     */
    void execute();


    /**
     * Enum representing all possible commands implemented by the system
     * <ul>
     *     <li>Command name from the tests</li>
     *     <li>Generator static method based on the {@code JsonNode}</li>
     * </ul>
     */
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

        /**
         * Converts a {@code String} to an {@code Command.Type}
         * @param label the string to convert
         * @return the corresponding {@code Command.Type}
         * @throws InputException if the label can't be converted to an {@code Command.Type}
         */
        public static Type fromString(final String label) throws InputException {
            try {
                return Arrays
                        .stream(Command.Type.values())
                        .filter(command -> command.label.equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown command: " + label);
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

        /**
         * Deserializes a JsonNode to a command matching the enum instance
         * @param node the JSON representation of the command
         * @return a deserialized {@code Command.Base} instance
         */
        public Command.Base parse(final JsonNode node) {
            return commandSupplier.apply(node);
        }
    }

    /**
     * Reads a command
     * @param node the JSON node representing the command
     * @return the deserialized node as a {@code Command} instance
     * @throws InputException if the node
     * could not be deserialized to an {@code Command} instance
     */
    static Command read(final JsonNode node) throws InputException {
        if (!node.isObject()) {
            throw new InputException("Command node is not an object");
        }

        Command.Type type = Command.Type.fromString(
                IOUtils.readStringChecked(node, "command")
        );

        int timestamp = IOUtils.readIntChecked(node, "timestamp");

        Command.Base command = type.parse(node);
        command.timestamp = timestamp;

        return command;
    }

    /**
     * Reads an array of commands
     * @param node the node containing the commands
     * @return a List of deserialized {@code Command} instances
     * @throws InputException if the given node is not an array
     */
    static List<Command> readArray(final JsonNode node) throws InputException {
        if (!node.isArray()) {
            throw new InputException("Commands list is not an array");
        }

        ArrayNode commands = (ArrayNode) node;
        List<Command> commandsList = new ArrayList<>();

        for (JsonNode commandNode : commands) {
            try {
                commandsList.add(read(commandNode));
            } catch (InputException e) {
                // If the command could not be read,
                // continue reading the other commands,
                // it's not a critical failing point
                if (BankingSystem.VERBOSE_LOGGING) {
                    System.err.println(e.getMessage());
                }
            }
        }

        return commandsList;
    }

    @Getter
    abstract class Base implements Command {

        protected Type command;
        protected int timestamp;

        public Base(final Type command) {
            this.command = command;
        }

        /**
         * Produces a {@code JsonNode} with the command name and timestamp
         * and a {@code output} object to be populated by the given consumer
         * The node is added to the StateWriter's buffer to be printed
         * @param consumer the functional interface to fill the {@code output} object
         */
        protected void output(final Consumer<ObjectNode> consumer) {
            ObjectNode root = StateWriter.getMapper().createObjectNode();
            root.put("timestamp", timestamp);
            root.put("command", command.toString());

            // Create the output node and send it to the consumer to populate it
            consumer.accept(root.putObject("output"));

            StateWriter.write(root);
        }

        /**
         * Produces a {@code JsonNode} with the command name and timestamp
         * and a {@code output} array to be populated by the given consumer
         * The node is added to the StateWriter's buffer to be printed
         * @param consumer the functional interface to fill the {@code output} array
         */
        protected void outputArray(final Consumer<ArrayNode> consumer) {
            ObjectNode root = StateWriter.getMapper().createObjectNode();
            root.put("timestamp", timestamp);
            root.put("command", command.toString());

            // Create the output node and send it to the consumer to populate it
            consumer.accept(root.putArray("output"));

            StateWriter.write(root);
        }

    }

}
