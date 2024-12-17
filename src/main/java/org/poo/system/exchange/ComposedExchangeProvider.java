package org.poo.system.exchange;

import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.ExchangeException;
import org.poo.utils.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public class ComposedExchangeProvider implements ExchangeProvider {

    private final Set<String> registeredCurrencies = new HashSet<>();
    private final List<Exchange> exchanges = new ArrayList<>();

    /**
     * Calculates all rates from a currency to another
     */
    public void computeComposedRates() {
        // Let's just assume we don't have an infinite money glitch
        // (that somehow the path leads back to the same currency with a bigger rate)
        if (exchanges.isEmpty()) {
            return;
        }
        Graph<String> currencyGraph = new Graph<>();

        exchanges.forEach(exchange ->
                currencyGraph.addEdge(
                        exchange.from(),
                        exchange.to(),
                        exchange.rate()
                )
        );

        currencyGraph.computePaths(
                (firstWeight, secondWeight) -> firstWeight * secondWeight
        ).forEach(
                (key, value) -> exchanges.add(
                        new Exchange(
                                key.first(),
                                key.second(),
                                value
                        )
                )
        );

    }

    /**
     * {@inheritDoc}.
     * Doesn't recalculate the composed rates
     */
    @Override
    public void registerExchange(final Exchange exchange) {


        registerCurrency(exchange.from());
        registerCurrency(exchange.to());

        exchanges.add(exchange);
        exchanges.add(exchange.reversed());
    }

    /**
     * {@inheritDoc}.
     * Recalculates the composed rates
     */
    @Override
    public void registerExchanges(final List<Exchange> exchangesList) {
        exchangesList.forEach(this::registerExchange);
        computeComposedRates();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void registerCurrency(final String currency) {
        registeredCurrencies.add(currency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String verifyCurrency(final String currency) throws BankingInputException {
        if (registeredCurrencies.contains(currency)) {
            return currency;
        }

        throw new BankingInputException("Invalid currency: " + currency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getRate(final String from, final String to) throws ExchangeException {
        if (from.equals(to)) {
            return 1.0;
        }
        try {
            return exchanges.parallelStream().filter(ex ->
                    ex.from().equals(from)
                            && ex.to().equals(to)
            ).toList().getFirst().rate();
        } catch (NoSuchElementException e) {
            throw new ExchangeException("No rate found for " + from + " -> " + to);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void printRates() {
        System.out.println("Composed rates: ");
        exchanges
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
