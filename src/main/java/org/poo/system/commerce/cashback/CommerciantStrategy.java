package org.poo.system.commerce.cashback;

import org.poo.system.commerce.Commerciant;
import org.poo.system.exceptions.InputException;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Optional;

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
     * @return the new total
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
         * Retrieves the commerciant data from an account if it matches the type
         * </br>
         * Otherwise, it creates a new data entry
         * @param account the account to search through
         * @return the account's commerciant data
         */
        protected CommerciantData getCommerciantData(final Account account) {

            Optional<CommerciantData> oData = account.getCommerciantData().stream().filter(
                    cData -> cData.getType() == commerciant.getType()
            ).findFirst();

            CommerciantData data;

            if (oData.isEmpty()) {
                data = new CommerciantData(commerciant.getType(), 0, 0);
                account.getCommerciantData().add(data);
            } else {
                data = oData.get();
            }

            return data;
        }

        /**
         * Retrieves the amount spent to the `spendingThreshold` category
         * @param account the account to retrieve de total spendings
         * @return the total spendings made by the given account
         */
        protected double getTotalSpending(final Account account) {
            double totalSpending = 0;

            for (CommerciantData data : account.getCommerciantData()) {
                totalSpending += data.getSpending();
            }

            return totalSpending;
        }

    }


}
