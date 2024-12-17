package org.poo.system.user;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.Transaction;
import org.poo.system.exceptions.BankingInputException;
import org.poo.utils.NodeConvertable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Getter
public class Account implements NodeConvertable {

    public enum Type {
        CLASSIC,
        SAVINGS;


        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        /**
         * Converts a String to an {@code Account.Type}
         * @param label the string to convert
         * @return the corresponding {@code Account.Type}
         * @throws BankingInputException if the label can't be converted to an {@code Account.Type}
         */
        public static Account.Type fromString(final String label) throws BankingInputException {
            try {
                return Arrays
                        .stream(Account.Type.values())
                        .filter(command -> command.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new BankingInputException("Unknown account type: " + label);
            }
        }

    }


    private final User owner;

    private final String accountIBAN;
    private final String currency;
    private final Type accountType;

    @Setter
    private String alias = "";
    @Setter
    private double interest;
    @Setter
    private double funds = 0;
    @Setter
    private double minBalance = 0;

    private final List<Card> cards = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public Account(
            final User owner,
            final String accountIBAN,
            final String currency,
            final Type accountType
    ) {
        this.owner = owner;
        this.accountIBAN = accountIBAN;
        this.currency = currency;
        this.accountType = accountType;
    }

    /**
     * @return the JSON representation of the Account
     */
    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("IBAN", accountIBAN);
        root.put("currency", currency);
        root.put("type", accountType.toString());
        root.put("balance", funds);

        ArrayNode cardsNode = root.putArray("cards");
        for (Card card : cards) {
            cardsNode.add(card.toNode());
        }

        return root;
    }

}
