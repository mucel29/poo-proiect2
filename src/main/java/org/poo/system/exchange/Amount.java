package org.poo.system.exchange;

import org.poo.system.BankingSystem;

public record Amount(double total, String currency) {

    /**
     * Converts the current amount to the given currency
     * @param newCurrency
     * @return
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
     * @param value
     * @return
     */
    public Amount sub(final double value) {
        return new Amount(total - value, currency);
    }

    /**
     * Adds the value to the amount
     * @param value
     * @return
     */
    public Amount add(final double value) {
        return new Amount(total + value, currency);
    }

    /**
     * Subtracts another amount from this one, making the conversion from other to this
     * @param other
     * @return
     */
    public Amount sub(final Amount other) {
        if (currency.equals(other.currency)) {
            return this.sub(other.total);
        }

        return new Amount(total - other.to(currency).total, currency);
    }

    /**
     * Adds another amount to this one, making the conversion from other to this
     * @param other
     * @return
     */
    public Amount add(final Amount other) {
        if (currency.equals(other.currency)) {
            return this.add(other.total);
        }

        return new Amount(total + other.to(currency).total, currency);
    }

    /**
     * Creates a new amount from the existing one with the new total
     * @param newTotal
     * @return
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
