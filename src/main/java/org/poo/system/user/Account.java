package org.poo.system.user;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.Exchange;
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


    private String IBAN;
    private String alias = "";
    private Exchange.Currency currency;
    private Type accountType;

    private double interest;
    private double funds;
    private double minBalance = 0;

    private final List<Card> cards = new ArrayList<>();

    public Account(String IBAN, Exchange.Currency currency, Type accountType) {
        this.IBAN = IBAN;
        this.currency = currency;
        this.accountType = accountType;
        this.funds = 0;
    }

    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("IBAN", IBAN);
        root.put("currency", currency.name());
        root.put("type", accountType.toString());
        root.put("balance", funds);

        ArrayNode cardsNode = root.putArray("cards");
        for (Card card : cards) {
            cardsNode.add(card.toNode());
        }

        return root;
    }

}
