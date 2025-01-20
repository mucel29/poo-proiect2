package org.poo.io;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.exceptions.InputException;
import org.poo.utils.Utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class IOUtils {

    private IOUtils() {

    }

    /**
     * Reads a field and checks if it's a String
     * @param node the node to read from
     * @param fieldName the field to read
     * @return the field's value
     * @throws InputException if the field is not present, or it is not a String
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
        // Check if the value is a string and not empty
        if (value == null) {
            throw new InputException(
                    fieldName + " is not a String\n" + node.toPrettyString()
            );
        }

        return value;
    }

    /**
     * Reads a field and checks if it's an int
     * @param node the node to read from
     * @param fieldName the field to read
     * @return the field's value
     * @throws InputException if the field is not present, or it is not an int
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
        // Check if the value is an integer
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
     * @throws InputException if the field is not present, or it is not a double
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
        // Check if the value is a double
        if (value < 0) {
            throw new InputException(fieldName + " is invalid\n" + node.toPrettyString());
        }

        return value;
    }

    /**
     * Reads a field and checks if it's a {@code LocalDate}
     *
     * @param node the node to read from
     * @param fieldName the field to read
     * @return the field's value
     * @throws InputException if the field is not present, or it is not a valid date
     */
    public static LocalDate readDateChecked(
            final JsonNode node,
            final String fieldName
    ) throws InputException {
        String date = IOUtils.readStringChecked(node, fieldName);

        try {
            return Utils.parseDate(date);
        } catch (DateTimeParseException e) {
            throw new InputException(fieldName + " is invalid\n" + node.toPrettyString());
        }
    }

}
