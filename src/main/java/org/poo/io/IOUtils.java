package org.poo.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.exceptions.BankingInputException;

public class IOUtils {

    private IOUtils() {

    }

    public static String readStringChecked(final JsonNode node, String fieldName) throws BankingInputException {
        JsonNode valueNode = node.get(fieldName);
        // Check node if it's null
        if (valueNode == null) {
            throw new BankingInputException(fieldName + " not found\n" + node.toPrettyString());
        }

        String value = valueNode.asText();
        if (value.isEmpty()) {
            throw new BankingInputException(
                    fieldName + " is empty or not a String\n" + node.toPrettyString()
            );
        }

        return value;
    }

    public static int readIntChecked(final JsonNode node, String fieldName) throws BankingInputException {
        JsonNode valueNode = node.get(fieldName);
        // Check node if it's null
        if (valueNode == null) {
            throw new BankingInputException(fieldName + " not found\n" + node.toPrettyString());
        }
        int value = valueNode.asInt(-1);
        if (value < 0) {
            throw new BankingInputException(fieldName + " is invalid\n" + node.toPrettyString());
        }

        return value;
    }

    public static double readDoubleChecked(final JsonNode node, String fieldName) throws BankingInputException {
        JsonNode valueNode = node.get(fieldName);
        // Check node if it's null
        if (valueNode == null) {
            throw new BankingInputException(fieldName + " not found\n" + node.toPrettyString());
        }
        double value = valueNode.asDouble(-1);
        if (value < 0) {
            throw new BankingInputException(fieldName + " is invalid\n" + node.toPrettyString());
        }

        return value;
    }

}
