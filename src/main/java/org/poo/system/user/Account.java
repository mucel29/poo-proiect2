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

@Getter @Setter
public class Account implements NodeConvertable {

    public enum Type {
        CLASSIC,
        SAVINGS;


        public static List<String> possibleValues() {
            return Arrays.stream(Type.values()).map((Type::toString)).toList();
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        public static Account.Type fromString(String label) throws BankingInputException {
            try {
                return Arrays.stream(Account.Type.values()).filter(command -> command.toString().equals(label)).toList().getFirst();
            } catch (NoSuchElementException e) {
                throw new BankingInputException("Unknown account type: " + label);
            }
        }

    }


    private User owner;

    private String IBAN;
    private String alias = "";
    private String currency;
    private Type accountType;

    private double interest;
    private double funds = 0;
    private double minBalance = 0;

    private final List<Card> cards = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();

    public Account(User owner, String IBAN, String currency, Type accountType) {
        this.owner = owner;
        this.IBAN = IBAN;
        this.currency = currency;
        this.accountType = accountType;
    }

    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("IBAN", IBAN);
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
