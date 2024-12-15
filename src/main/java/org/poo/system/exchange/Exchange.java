package org.poo.system.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.poo.io.IOUtils;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.ExchangeException;

import java.util.*;


@Getter
public class Exchange {

    // TODO: Offload the methods from BankingSystem to static Exchange methods

    private final String from;
    private final String to;
    private final double rate;

    public Exchange(String from, String to, double rate) {
        this.from = from;
        this.to = to;
        this.rate = rate;
    }

    // Static / Utility part

    private static final Set<String> registeredCurrencies = new HashSet<>();
    private static final List<Exchange> exchanges = new ArrayList<>();

    public static void reset() {
        registeredCurrencies.clear();
        exchanges.clear();
    }

    private static void registerExchange(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("Exchange rate is not an object node");
        }

        String from = IOUtils.readStringChecked(node, "from");
        String to = IOUtils.readStringChecked(node, "to");
        double rate = IOUtils.readDoubleChecked(node, "rate");

        registeredCurrencies.add(from);
        registeredCurrencies.add(to);

        exchanges.add(new Exchange(from, to, rate));
        exchanges.add(new Exchange(to, from, 1.0 / rate));
    }

    public static void registerExchanges(final JsonNode node) throws BankingInputException {
        if (!node.isArray()) {
            throw new BankingInputException("Exchange rates is not an array node");
        }
        for (JsonNode exchange : node) {
            registerExchange(exchange);
        }
        computeComposedRates();
    }

    private static void computeComposedRates() {
        // Let's just assume we don't have an infinite money glitch
        // (that somehow the path leads back to the same currency with a bigger rate)
        if (exchanges.isEmpty()) {
            return;
        }
        Graph<String> currencyGraph = new Graph<>();

        exchanges.forEach(
                exchange -> currencyGraph
                        .addEdge(
                                exchange.getFrom(),
                                exchange.getTo(),
                                exchange.getRate()
                        )
        );

        currencyGraph.computePaths().forEach((key, value) -> exchanges.add(new Exchange(key.getFirst(), key.getSecond(), value)));

    }

    public static void registerCurrency(final String currency) {
        registeredCurrencies.add(currency);
    }

    public static String verifyCurrency(final String currency) throws BankingInputException {
        if (registeredCurrencies.contains(currency)) {
            return currency;
        }

        throw new BankingInputException("Invalid currency: " + currency);
    }

    public static double getRate(final String from, final String to) throws ExchangeException {
        if (from.equals(to)) {
            return 1.0;
        }
        try {
            return exchanges.parallelStream().filter(ex -> ex.getFrom().equals(from) && ex.getTo().equals(to)).toList().getFirst().getRate();
        } catch (NoSuchElementException e) {
            throw new ExchangeException("No rate found for " + from + " -> " + to);
        }
    }

    public static void printRates() {
        System.out.println("Composed rates: ");
        exchanges.parallelStream().forEach(exchange -> System.out.println(exchange.getFrom() + " -> " + exchange.getTo() + " [" + exchange.getRate() + "]"));
    }

}
