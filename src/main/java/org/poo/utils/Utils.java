package org.poo.utils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Random;
import java.util.regex.Pattern;

public final class Utils {
    private Utils() {
        // Checkstyle error free constructor
    }

    private static final int IBAN_SEED = 1;
    private static final int CARD_SEED = 2;
    private static final int DIGIT_BOUND = 10;
    private static final int DIGIT_GENERATION = 16;
    private static final String RO_STR = "RO";
    private static final String POO_STR = "POOB";


    private static Random ibanRandom = new Random(IBAN_SEED);
    private static Random cardRandom = new Random(CARD_SEED);

    /**
     * Utility method for generating an IBAN code.
     *
     * @return the IBAN as String
     */
    public static String generateIBAN() {
        StringBuilder sb = new StringBuilder(RO_STR);
        for (int i = 0; i < RO_STR.length(); i++) {
            sb.append(ibanRandom.nextInt(DIGIT_BOUND));
        }

        sb.append(POO_STR);
        for (int i = 0; i < DIGIT_GENERATION; i++) {
            sb.append(ibanRandom.nextInt(DIGIT_BOUND));
        }

        return sb.toString();
    }

    private static final Pattern IBAN_REGEX = Pattern.compile(
            RO_STR + "[0-9]{" + RO_STR.length() + "}"
                    + POO_STR + "[0-9]{" + DIGIT_GENERATION + "}"
    );

    /**
     * Utility method for verifying if a string is a valid IBAN
     *
     * @param iban the string to check
     * @return whether the string is an IBAN or not
     */
    public static boolean verifyIBAN(final String iban) {
        return IBAN_REGEX.matcher(iban).matches();
    }

    /**
     * Utility method for generating a card number.
     *
     * @return the card number as String
     */
    public static String generateCardNumber() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < DIGIT_GENERATION; i++) {
            sb.append(cardRandom.nextInt(DIGIT_BOUND));
        }

        return sb.toString();
    }

    /**
     * Parses a {@code LocalDate}
     *
     * @throws DateTimeParseException if the date could not be parsed
     */
    public static LocalDate parseDate(final String dateString) throws DateTimeParseException {
        return LocalDate.parse(dateString);
    }

    /**
     * Resets the seeds between runs.
     */
    public static void resetRandom() {
        ibanRandom = new Random(IBAN_SEED);
        cardRandom = new Random(CARD_SEED);
    }
}
