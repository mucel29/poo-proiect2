package org.poo.system;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exchange.Graph;
import org.poo.system.exchange.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


@Getter
public class Exchange {

    public enum Currency {
        USD,
        EUR,
        RON;

        public static List<String> possibleValues() {
            return Arrays.stream(Currency.values()).map(Currency::name).toList();
        }

        public static Currency fromString(String currency) throws BankingInputException {
            try {
                return Currency.valueOf(currency.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BankingInputException("Invalid currency: " + currency);
            }
        }

    }

    private final Currency from;
    private final Currency to;
    private final double rate;

    public Exchange(Currency from, Currency to, double rate) {
        this.from = from;
        this.to = to;
        this.rate = rate;
    }

    public static Exchange read(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("Exchange rate is not an object node");
        }

        Currency from = Currency.fromString(node.get("from").asText());
        Currency to = Currency.fromString(node.get("to").asText());
        double rate = node.get("rate").asDouble(-1);
        if (rate < 0) {
            throw new BankingInputException("Exchange rate not present for " + from + " -> " + to + " conversion");
        }

        return new Exchange(from, to, rate);
    }

    public static List<Exchange> readArray(final JsonNode node) throws BankingInputException {
        if (!node.isArray()) {
            throw new BankingInputException("Exchange rates is not an array node");
        }
        List<Exchange> exchanges = new ArrayList<>();
        for (JsonNode exchange : node) {
            exchanges.add(Exchange.read(exchange));
        }

        return exchanges;
    }

    public static void computeComposedRates(List<Exchange> exchanges) {
        // Let's just assume all the exchange rates are <= 1 so we don't have infinite money glitch

        Graph<Currency> currencyGraph = new Graph<>();

        for (Exchange exchange : exchanges) {
            currencyGraph.addEdge(exchange.getFrom(), exchange.getTo(), exchange.getRate());
        }

        Map<Pair<Currency, Currency>, Double> composedRates = currencyGraph.computePaths();
        // Used var, type is way too long
        for (var entry : composedRates.entrySet()) {
            exchanges.add(new Exchange(entry.getKey().getFirst(), entry.getKey().getSecond(), entry.getValue()));
        }

    }

}
