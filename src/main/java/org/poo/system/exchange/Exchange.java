package org.poo.system.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.exceptions.InputException;

import java.util.ArrayList;
import java.util.List;


public record Exchange(String from, String to, double rate) {

    /**
     * Creates a reversed exchange using the current instance
     * @return a new {@code Exchange} instance in the other direction
     */
    public Exchange reversed() {
        return new Exchange(this.to, this.from, 1.0 / this.rate);
    }

    /**
     * Deserializes an {@code Exchange}
     * @param node the JSON representation of the exchange
     * @throws InputException if an {@code Exchange} could not be deserialized
     */
    private static Exchange read(final JsonNode node) throws InputException {
        if (!node.isObject()) {
            throw new InputException("Exchange rate is not an object node");
        }

        String from = IOUtils.readStringChecked(node, "from");
        String to = IOUtils.readStringChecked(node, "to");
        double rate = IOUtils.readDoubleChecked(node, "rate");

        return new Exchange(from, to, rate);
    }



    /**
     * Deserializes a list of {@code Exchange}s
     * @param node the node containing the exchanges
     * @throws InputException if the node is not an array
     * or an {@code Exchange} could not be deserialized
     */
    public static List<Exchange> readArray(final JsonNode node) throws InputException {
        if (node == null) {
            throw new InputException("No exchange rates found");
        }

        if (!node.isArray()) {
            throw new InputException("Exchange rates is not an array node");
        }

        List<Exchange> exchanges = new ArrayList<>();

        for (JsonNode exchangeNode : node) {
            exchanges.add(read(exchangeNode));
        }

        return exchanges;
    }

}
