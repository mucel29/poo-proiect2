package org.poo.system.exchange;

import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.ExchangeException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public final class BasicExchangeProvider implements ExchangeProvider {

    private final Set<String> registeredCurrencies = new HashSet<>();
    private final List<Exchange> exchanges = new ArrayList<>();

    /**
     * {@inheritDoc}.
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
     */
    @Override
    public void registerExchanges(final List<Exchange> exchangesList) {
        exchangesList.forEach(this::registerExchange);
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
