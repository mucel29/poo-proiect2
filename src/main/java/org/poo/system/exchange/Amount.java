package org.poo.system.exchange;

import org.poo.system.BankingSystem;

public record Amount(double total, String currency) {

    /**
     * Creates a new {@code Amount} of 0 in the given currency
     * @param currency the amount's currency
     * @return a new {@code Amount} of 0
     */
    public static Amount zero(final String currency) {
        return new Amount(0, currency);
    }

    /**
     * Converts the current amount to the given currency
     * @param newCurrency
     * @return a new amount in the requested currency
     */
    public Amount to(final String newCurrency) {
        return new Amount(
                total
                        * BankingSystem.getExchangeProvider()
                            .getRate(this.currency, newCurrency),
                newCurrency
        );
    }

    /**
     * Subtracts the value from the amount
     *
     * @param value
     * @return a new amount with {@code value} subtracted from it
     */
    public Amount sub(final double value) {
        return new Amount(total - value, currency);
    }

    /**
     * Adds the value to the amount
     *
     * @param value
     * @return a new amount with {@code value} added to it
     */
    public Amount add(final double value) {
        return new Amount(total + value, currency);
    }

    /**
     * Subtracts another amount from this one, making the conversion from other to this
     *
     * @param other
     * @return a new amount with {@code other} subtracted from it
     */
    public Amount sub(final Amount other) {
        if (currency.equals(other.currency)) {
            return this.sub(other.total);
        }

        return new Amount(total - other.to(currency).total, currency);
    }

    /**
     * Adds another amount to this one, making the conversion from other to this
     *
     * @param other
     * @return a new amount with {@code other} added to it
     */
    public Amount add(final Amount other) {
        if (currency.equals(other.currency)) {
            return this.add(other.total);
        }

        return new Amount(total + other.to(currency).total, currency);
    }

    /**
     * Creates a new amount using
     * the instance's currency and the provided new total
     *
     * @param newTotal the value used for the new {@code Amount}
     * @return a new amount with the new total
     */
    public Amount set(final double newTotal) {
        return new Amount(newTotal, currency);
    }

    @Override
    public String toString() {
        return total + " " + currency;
    }

    @Override
    public Amount clone() {
        return new Amount(total, currency);
    }

}
