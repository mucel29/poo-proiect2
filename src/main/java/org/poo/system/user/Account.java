package org.poo.system.user;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.commerce.Commerciant;
import org.poo.system.commerce.cashback.CommerciantData;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exchange.Amount;
import org.poo.utils.NodeConvertable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@Getter
public class Account implements NodeConvertable {

    public enum Type {
        CLASSIC,
        SAVINGS,
        BUSINESS;


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


    protected final User owner;

    protected final String accountIBAN;
    protected final Type accountType;

    @Setter
    protected Amount spendingLimit;

    @Setter
    protected Amount depositLimit;

    @Setter
    private String alias = "";
    @Setter
    private double interest;
    @Setter
    protected Amount funds;
    @Setter
    protected double minBalance = 0;

    protected final List<Card> cards = new ArrayList<>();
    protected final List<Transaction> transactions = new ArrayList<>();
    protected final List<CommerciantData> commerciantData = new ArrayList<>();
    protected final Map<Commerciant.Type, Boolean> discounts = new HashMap<>();

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

        for (Commerciant.Type type : Commerciant.Type.values()) {
            discounts.put(type, false);
        }

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
     * @param user the user to check for authorization
     * @return whether the user is authorized
     */
    public boolean isAuthorized(final User user) {
        return owner.equals(user);
    }

    /**
     * @param user the user to check the authorization
     * @param card the card to delete
     * @throws OwnershipException if the user is not authorized for this account
     */
    public void authorizeCardDeletion(
            final User user,
            final Card card
    ) throws OwnershipException {
        if (!this.isAuthorized(user)) {
            throw new OwnershipException(
                    "User "
                            + user.getEmail()
                            + " is not authorized for "
                            + accountIBAN
            );
        }

        // Remove the card from storage
        BankingSystem.getStorageProvider().removeCard(card);
        BankingSystem.log("Deleted card: " + card.getCardNumber());
    }

    /**
     * Applies the owner's fee for the given amount
     * @param amount the amount to take the fee for
     */
    public void applyFee(final Amount amount) {
        funds = funds.sub(owner.getServicePlan().getFee(this, amount));
    }

    /**
     * Applies cashback for a given amount
     *
     * @param user the user who made the payment
     * @param commerciant the commerciant that was paid
     * @param amount the amount that was paid
     */
    public void applyCashBack(
            final User user,
            final Commerciant commerciant,
            final Amount amount
    ) {
        Amount cashback = commerciant.getStrategy().apply(
                this,
                amount
        );
        // 1.3775
        // 1.3195
        if (cashback.total() > 0.0) {
            BankingSystem.log(
                    accountIBAN + "[cashback]: "
                            + funds + " -> " + funds.add(cashback)
            );
        }

        funds = funds.add(cashback);
    }

    /**
     * @param user the user to check the authorization
     * @param amount the amount to deposit
     */
    public void authorizeDeposit(
            final User user,
            final Amount amount
    ) {
        Amount newBalance = funds.add(amount);

        BankingSystem.log(
                accountIBAN + "[deposit]: "
                        + funds + " -> " + newBalance
        );

        funds = newBalance;
    }

    /**
     * @param user the user to check the authorization
     * @param amount the amount to spend
     * @throws OperationException if the account doesn't have enough funds
     * or if it will go under the minimum
     */
    public void authorizeSpending(
            final User user,
            final Amount amount
    ) throws OperationException {
        Amount newBalance = funds.sub(amount);
        if (newBalance.total() < 0.0) {
            throw new OperationException("Insufficient funds");
        }
        if (newBalance.total() < minBalance) {
            throw new OperationException("Under minimum");
        }

        BankingSystem.log(
                accountIBAN + "[spending]: "
                + funds + " -> " + newBalance
        );

        funds = newBalance;
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
