package org.poo.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.exceptions.InputException;

public final class IOUtils {

    private IOUtils() {

    }

    /**
     * Reads a field and checks if it's a String
     * @param node the node to read from
     * @param fieldName the field to read
     * @return the field's value
     * @throws InputException if the field is not present or it is not a String
     */
    public static String readStringChecked(
            final JsonNode node,
            final String fieldName
    ) throws InputException {
        JsonNode valueNode = node.get(fieldName);
        // Check node if it's null
        if (valueNode == null) {
            throw new InputException(fieldName + " not found\n" + node.toPrettyString());
        }

        String value = valueNode.asText();
        if (value.isEmpty()) {
            throw new InputException(
                    fieldName + " is empty or not a String\n" + node.toPrettyString()
            );
        }

        return value;
    }

    /**
     * Reads a field and checks if it's an int
     * @param node the node to read from
     * @param fieldName the field to read
     * @return the field's value
     * @throws InputException if the field is not present or it is not an int
     */
    public static int readIntChecked(
            final JsonNode node,
            final String fieldName
    ) throws InputException {
        JsonNode valueNode = node.get(fieldName);
        // Check node if it's null
        if (valueNode == null) {
            throw new InputException(fieldName + " not found\n" + node.toPrettyString());
        }
        int value = valueNode.asInt(-1);
        if (value < 0) {
            throw new InputException(fieldName + " is invalid\n" + node.toPrettyString());
        }

        return value;
    }

    /**
     * Reads a field and checks if it's a double
     * @param node the node to read from
     * @param fieldName the field to read
     * @return the field's value
     * @throws InputException if the field is not present or it is not a double
     */
    public static double readDoubleChecked(
            final JsonNode node,
            final String fieldName
    ) throws InputException {
        JsonNode valueNode = node.get(fieldName);
        // Check node if it's null
        if (valueNode == null) {
            throw new InputException(fieldName + " not found\n" + node.toPrettyString());
        }
        double value = valueNode.asDouble(-1);
        if (value < 0) {
            throw new InputException(fieldName + " is invalid\n" + node.toPrettyString());
        }

        return value;
    }

}
