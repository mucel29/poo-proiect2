package org.poo.system.payments;

import org.poo.system.exceptions.InputException;

import java.util.Arrays;
import java.util.NoSuchElementException;

public interface PendingPayment {

    enum Type {
        EQUAL,
        CUSTOM;

        @Override
        public String toString() {
            return name().toLowerCase();
        }

        /**
         * Converts a String to an {@code SplitPayCommand.Type}
         *
         * @param label the string to convert
         * @return the corresponding {@code SplitPayCommand.Type}
         * @throws InputException if the label can't be converted to an {@code SplitPayCommand.Type}
         */
        public static PendingPayment.Type fromString(final String label) throws InputException {
            try {
                return Arrays
                        .stream(PendingPayment.Type.values())
                        .filter(splitType -> splitType.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown payment type: " + label);
            }
        }
    }

    /**
     * @return the payment's type
     */
    PendingPayment.Type getType();

    /**
     * Accepts the payment
     *
     * @param observer the one observing the payment
     */
    void accept(PaymentObserver observer);

    /**
     * Rejects the payment
     *
     * @param observer the one observing the payment
     */
    void reject(PaymentObserver observer);

    /**
     * Checks whether the payment was dealt with by the given observer
     *
     * @param observer the one observing the payment
     * @return if the payment was dealt with by the observer
     */
    boolean wasDealt(PaymentObserver observer);

}
