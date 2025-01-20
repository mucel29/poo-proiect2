package org.poo.system.commerce.cashback;

import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.exceptions.InputException;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

public interface CommerciantStrategy {

    enum Type {
        SPENDING("spendingThreshold"),
        TRANSACTIONS("nrOfTransactions");

        private final String inputLabel;

        Type(final String inputLabel) {
            this.inputLabel = inputLabel;
        }

        @Override
        public String toString() {
            return this.inputLabel;
        }

        /**
         * Converts a String to an {@code CommerciantStrategy.Type}
         * @param label the string to convert
         * @return the corresponding {@code CommerciantStrategy.Type}
         * @throws InputException if the label can't be converted to an
         * {@code CommerciantStrategy.Type}
         */
        public static CommerciantStrategy.Type fromString(
                final String label
        ) throws InputException {
            try {
                return Arrays
                        .stream(CommerciantStrategy.Type.values())
                        .filter(command -> command.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown cashback strategy: " + label);
            }
        }

    }

    /**
     * Applies the strategy on the given account
     *
     * @param account the account on which to apply the strategy on
     * @param amount the total to be paid
     *
     * @return the value of the cashback
     */
    Amount apply(Account account, Amount amount);

    abstract class Base implements CommerciantStrategy {
        protected final Commerciant commerciant;

        protected Base(
                final Commerciant commerciant
        ) {
            this.commerciant = commerciant;
        }

        /**
         * Retrieves the amount spent to the `spendingThreshold` category
         *
         * @param account the account to retrieve de total spendings
         * @return the total spendings made by the given account
         */
        protected double getTotalSpending(final Account account) {
            return BankingSystem
                    .getStorageProvider()
                    .getCommerciants()
                    .stream()
                    .map(comm -> comm.getSpendings(account))
                    .reduce(0.0, Double::sum);
        }

        /**
         * Checks if the given account is eligible for a coupon and applies it
         *
         * @param account the account to check
         * @param amount the amount of the transaction
         *
         * @return the coupon cashback or 0
         */
        protected Amount applyCoupon(
                final Account account,
                final Amount amount
        ) {

            Map<Commerciant.Type, Boolean> coupons = account.getCoupons();

            // Check if the coupon was already redeemed, or it isn't available yet
            if (!coupons.containsKey(commerciant.getType())
                    || !coupons.get(commerciant.getType())) {
                return Amount.zero(account.getCurrency());
            }

            // Remove the coupon, marking the coupon as redeemed
            account.getCoupons().remove(commerciant.getType());

            // Calculate the cashback total
            Amount couponCashback = new Amount(
                    amount.total()
                            * commerciant.getType().getTransactionCashback(),
                    amount.currency()
            );

            BankingSystem.log(
                    "Applied coupon to "
                            + account.getAccountIBAN()
                            + " ["
                            + couponCashback
                            + "]"
            );

            return couponCashback;

        }

    }


}
