package org.poo.system.exchange;

import org.poo.system.exceptions.BankingInputException;

import java.util.List;

public interface ExchangeProvider {

    /**
     * Registers an exchange and it's reversed exchange
     * @param exchange the exchange to register
     */
    void registerExchange(Exchange exchange);

    /**
     * Registers a list of exchanges and their reversed exchanges
     * @param exchanges the list to register
     */
    void registerExchanges(List<Exchange> exchanges);

    /**
     * Registers a currency
     *
     * @param currency the currency to register
     */
    void registerCurrency(String currency);

    /**
     * Verifies if the given string is a registered currency
     *
     * @param currency the currency to verify
     * @return the currency string is it's valid
     * @throws BankingInputException if the currency is not a registered currency
     */
    String verifyCurrency(String currency) throws BankingInputException;

    /**
     * Finds the rate from a given currency to another
     *
     * @param from the currency to exchange from
     * @param to   the currency to exchange to
     * @return the rate of exchange from {@code from} to {@code to}
     * @throws ExchangeException if no exchange between the two currencies exists
     */
    double getRate(String from, String to) throws ExchangeException;

    /**
     * Prints all rates from a currency to any other
     */
    void printRates();
}
