package org.poo.system.user;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.commerce.cashback.CommerciantData;
import org.poo.system.exceptions.InputException;
import org.poo.system.exchange.Amount;
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
         * @throws InputException if the label can't be converted to an {@code Account.Type}
         */
        public static Account.Type fromString(final String label) throws InputException {
            try {
                return Arrays
                        .stream(Account.Type.values())
                        .filter(command -> command.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown account type: " + label);
            }
        }

    }


    private final User owner;

    private final String accountIBAN;
    private final Type accountType;

    @Setter
    private String alias = "";
    @Setter
    private double interest;
    @Setter
    private Amount funds;
    @Setter
    private double minBalance = 0;

    private final List<Card> cards = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<CommerciantData> commerciantData = new ArrayList<>();

    public Account(
            final User owner,
            final String accountIBAN,
            final String currency,
            final Type accountType
    ) {
        this.owner = owner;
        this.accountIBAN = accountIBAN;
        this.funds = new Amount(0, currency);
        this.accountType = accountType;
    }

    /**
     * Here just for logging
     */
    public Account setFunds(final Amount newFunds) {
        BankingSystem.log(
                "[" + owner.getEmail() + "]: " + accountIBAN
                + " set funds from " + this.funds.total() + " to " + newFunds.total()
        );
        this.funds = newFunds;
        return this;
    }

    /**
     * @return the account's currency
     */
    public String getCurrency() {
        return this.funds.currency();
    }

    /**
     * @param amount the amount to be paid
     * @param canGoUnderMinimum if the funds can go under the minimum
     * @return whether the account can pay the given amount
     */
    public boolean canPay(
            final Amount amount,
            final boolean canGoUnderMinimum
    ) {
        Amount newBalance = funds.sub(amount);
        if (!canGoUnderMinimum) {
            return newBalance.total() > minBalance;
        }

        return newBalance.total() > 0.0;
    }

    /**
     * @return the JSON representation of the Account
     */
    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("IBAN", accountIBAN);
        root.put("currency", funds.currency());
        root.put("type", accountType.toString());
        root.put("balance", funds.total());

        ArrayNode cardsNode = root.putArray("cards");
        for (Card card : cards) {
            cardsNode.add(card.toNode());
        }

        return root;
    }

}
