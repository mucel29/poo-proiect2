package org.poo.system.exchange;

import org.poo.system.BankingSystem;
import org.poo.system.exceptions.InputException;
import org.poo.utils.Graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An {@code ExceptionProvider} implementation that stores the registered exchanges
 * and their reversed counterparts
 * </br>
 * Can compute indirect exchanges
 */
public final class ComposedExchange implements ExchangeProvider {

    private final Set<String> registeredCurrencies = new HashSet<>();
    private final List<Exchange> exchanges = new ArrayList<>();

    /**
     * Calculates all rates from a currency to another
     */
    public void computeComposedRates() {
        // Let's just assume we don't have an infinite money glitch
        // (that somehow the path leads back to the same currency with a better rate)
        if (exchanges.isEmpty()) {
            return;
        }

        // Create a weighted graph and fill it with the exchanges
        Graph<String> currencyGraph = new Graph<>();

        exchanges.forEach(exchange ->
                currencyGraph.addEdge(
                        exchange.from(),
                        exchange.to(),
                        exchange.rate()
                )
        );

        // Calculate the indirect exchanges using the `PathComposer` lambda
        // and add them to the stored exchanges
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
    public String verifyCurrency(final String currency) throws InputException {
        if (registeredCurrencies.contains(currency)) {
            return currency;
        }

        throw new InputException("Invalid currency: " + currency);
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
        BankingSystem.log("Composed rates: ");
        exchanges
                .parallelStream()
                .forEach(
                        exchange -> BankingSystem.log(
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
