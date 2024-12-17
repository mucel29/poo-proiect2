package org.poo.system.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.ExchangeException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


public record Exchange(String from, String to, double rate) {

    // Static / Utility part
    private static final Set<String> REGISTERED_CURRENCIES = new HashSet<>();
    private static final List<Exchange> EXCHANGES = new ArrayList<>();

    /**
     * resets the exchange data
     */
    public static void reset() {
        REGISTERED_CURRENCIES.clear();
        EXCHANGES.clear();
    }

    /**
     * Deserializes an `Exchange`
     * @param node the JSON representation of the exchange
     * @throws BankingInputException if an `Exchange` could not be deserialized
     */
    private static void registerExchange(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("Exchange rate is not an object node");
        }

        String from = IOUtils.readStringChecked(node, "from");
        String to = IOUtils.readStringChecked(node, "to");
        double rate = IOUtils.readDoubleChecked(node, "rate");

        REGISTERED_CURRENCIES.add(from);
        REGISTERED_CURRENCIES.add(to);

        EXCHANGES.add(new Exchange(from, to, rate));
        EXCHANGES.add(new Exchange(to, from, 1.0 / rate));
    }

    /**
     * Deserializes a list of `Exchange`s
     * @param node the node containing the exchanges
     * @throws BankingInputException if the node is not an array
     * or an `Exchange` could not be deserialized
     */
    public static void registerExchanges(final JsonNode node) throws BankingInputException {
        if (!node.isArray()) {
            throw new BankingInputException("Exchange rates is not an array node");
        }
        for (JsonNode exchange : node) {
            registerExchange(exchange);
        }
        computeComposedRates();
    }

    /**
     * Calculates all rates from a currency to another
     */
    private static void computeComposedRates() {
        // Let's just assume we don't have an infinite money glitch
        // (that somehow the path leads back to the same currency with a bigger rate)
        if (EXCHANGES.isEmpty()) {
            return;
        }
        Graph<String> currencyGraph = new Graph<>();

        EXCHANGES.forEach(exchange ->
                currencyGraph.addEdge(
                        exchange.from(),
                        exchange.to(),
                        exchange.rate()
                )
        );

        currencyGraph.computePaths(
                (firstWeight, secondWeight) -> firstWeight * secondWeight
        ).forEach(
                (key, value) -> EXCHANGES.add(
                        new Exchange(
                                key.first(),
                                key.second(),
                                value
                        )
                )
        );

    }

    /**
     * Registers a currency
     * @param currency the currency to register
     */
    public static void registerCurrency(final String currency) {
        REGISTERED_CURRENCIES.add(currency);
    }

    /**
     * Verifies if the given string is a registered currency
     * @param currency the currency to verify
     * @return the currency string is it's valid
     * @throws BankingInputException if the currency is not a registered currency
     */
    public static String verifyCurrency(final String currency) throws BankingInputException {
        if (REGISTERED_CURRENCIES.contains(currency)) {
            return currency;
        }

        throw new BankingInputException("Invalid currency: " + currency);
    }

    /**
     * Finds the rate from a given currency to another
     * @param from the currency to exchange from
     * @param to the currency to exchange to
     * @return the rate of exchange from `from` to `to`
     * @throws ExchangeException if no exchange between the two currencies exists
     */
    public static double getRate(final String from, final String to) throws ExchangeException {
        if (from.equals(to)) {
            return 1.0;
        }
        try {
            return EXCHANGES.parallelStream().filter(ex ->
                            ex.from().equals(from)
                            && ex.to().equals(to)
            ).toList().getFirst().rate();
        } catch (NoSuchElementException e) {
            throw new ExchangeException("No rate found for " + from + " -> " + to);
        }
    }

    /**
     * Prints all rates from a currency to any other
     */
    public static void printRates() {
        System.out.println("Composed rates: ");
        EXCHANGES
                .parallelStream()
                .forEach(
                        exchange -> System.out.println(
                                exchange.from()
                                        + " -> "
                                        + exchange.to()
                                        + " ["
                                        + exchange.rate()
                                        + "]"
                        )
                );
    }

}
