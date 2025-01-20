package org.poo.system.commerce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.commerce.cashback.CommerciantStrategy;
import org.poo.system.commerce.cashback.StrategyFactory;
import org.poo.system.exceptions.InputException;
import org.poo.system.user.Account;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Getter
public final class Commerciant {

    @Getter
    public enum Type {
        FOOD(2, 2e-2),
        CLOTHES(5, 2e-2),
        TECH(10, 10e-2);

        private final int transactionThreshold;
        private final double transactionCashback;

        Type(
                final int transactionThreshold,
                final double transactionCashback
        ) {
            this.transactionThreshold = transactionThreshold;
            this.transactionCashback = transactionCashback;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        /**
         * Converts a String to an {@code Commerciant.Type}
         * @param label the string to convert
         * @return the corresponding {@code Commerciant.Type}
         * @throws InputException if the label can't be converted to an {@code Commerciant.Type}
         */
        public static Commerciant.Type fromString(final String label) throws InputException {
            try {
                return Arrays
                        .stream(Commerciant.Type.values())
                        .filter(command -> command.toString().equals(label.toLowerCase()))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown commerciant type: " + label);
            }
        }

    }

    private final String name;
    private final int id;
    private final String accountIBAN;
    private final Type type;
    private final CommerciantStrategy strategy;

    private final Map<Account, Integer> transactions = new HashMap<>();

    public Commerciant(
            final String name,
            final int id,
            final String accountIBAN,
            final Type type,
            final CommerciantStrategy.Type strategy
    ) {
        this.name = name;
        this.id = id;
        this.accountIBAN = accountIBAN;
        this.type = type;
        this.strategy = StrategyFactory.getCommerciantStrategy(this, strategy);
    }

    /**
     * Retrieves the number of transactions made to this commerciant
     * @param account the account
     * @return the number of transaction the given account has made to this commerciant
     */
    public int getTransactionCount(final Account account) {
        return transactions.computeIfAbsent(account, k -> 0);
    }

    /**
     * Adds a transaction made by the given account to this commerciant
     * @param account
     */
    public void addTransaction(final Account account) {
        transactions.put(account, getTransactionCount(account) + 1);
    }

    /**
     * Reads a commerciant
     * @param node the JSON node representing the commerciant
     * @return the deserialized node as an {@code Commerciant}
     * @throws InputException if the node could not be deserialized to an
     * {@code Commerciant} instance
     */
    public static Commerciant read(final JsonNode node) throws InputException {
        if (!node.isObject()) {
            throw new InputException("Commerciant node is not an object");
        }

        String name = IOUtils.readStringChecked(node, "commerciant");
        int id = IOUtils.readIntChecked(node, "id");
        String account = IOUtils.readStringChecked(node, "account");
        Type type = Type.fromString(IOUtils.readStringChecked(node, "type"));
        CommerciantStrategy.Type strategy = CommerciantStrategy.Type.fromString(
                IOUtils.readStringChecked(node, "cashbackStrategy")
        );

        return new Commerciant(
                name,
                id,
                account,
                type,
                strategy
        );
    }

    /**
     * Reads an array of commerciants
     * @param node the node containing the users
     * @return a List of deserialized {@code Commerciant} instances
     * @throws InputException if the given node is not an array
     */
    public static List<Commerciant> readArray(final JsonNode node) throws InputException {
        if (!node.isArray()) {
            throw new InputException("Commerciant list is not an array");
        }

        ArrayNode commerciants = (ArrayNode) node;
        List<Commerciant> commerciantsList = new ArrayList<>();

        for (JsonNode commerciant : commerciants) {
            try {
                commerciantsList.add(read(commerciant));
            } catch (InputException e) {
                if (BankingSystem.VERBOSE_LOGGING) {
                    System.err.println(e.getDetailedMessage());
                }
            }
        }

        return commerciantsList;
    }


}
