package org.poo.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exchange.Exchange;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public final class BankingSystem {

    private final List<User> users = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();

    private final Map<String, User> accountMap = new HashMap<>();
    private final Map<String, String> aliasMap = new HashMap<>();

    private BankingSystem() {

    }

    private static BankingSystem instance;

    /**
     * @return the singleton instance
     * @throws IllegalStateException if the system is not initialized
     */
    public static BankingSystem getInstance() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException("System not initialized");
        }

        return instance;
    }

    private static int testNumber = 0;

    /**
     * Resets the system's state
     */
    private void reset() {
        users.clear();
        commands.clear();
        accountMap.clear();
        Exchange.reset();

        Utils.resetRandom();
    }


    /**
     * Initializes the system using a file
     * @param file json file with the users, exchanges and commands
     * @throws IOException if the file could not be read
     */
    public static void init(final File file) throws IOException {
        if (instance == null) {
            instance = new BankingSystem();
        }

        instance.reset();
        testNumber++;
        String jsonString = Files.readString(file.toPath());
        JsonNode root = new ObjectMapper().readTree(jsonString);

        // Read data from root

        // Read users
        JsonNode usersNode = root.get("users");
        if (usersNode == null) {
            throw new BankingInputException("No users found");
        }
        instance.getUsers().addAll(User.readArray(usersNode));

        // Read exchange rates
        JsonNode exchangeNode = root.get("exchangeRates");
        if (exchangeNode == null) {
            throw new BankingInputException("No exchange rates found");
        }
        // Add exchanges from input
        Exchange.registerExchanges(exchangeNode);
        Exchange.printRates();



        // Read commands
        JsonNode commandsNode = root.get("commands");
        if (commandsNode == null) {
            throw new BankingInputException("No commands found");
        }
        instance.getCommands().addAll(Command.readArray(commandsNode));

    }

    /**
     * Runs the system's stored commands
     */
    public static void run() {
        for (Command command : instance.getCommands()) {
            try {
                command.execute();
            } catch (Exception e) {
                // Todo: do some handling here ig
                System.err.println("[" + testNumber + "] Caught exception: " + e.getMessage());
            }
        }
    }

    /**
     * Finds an user
     * @param email the email associated with the user
     * @return the requested user
     * @throws UserNotFoundException if the user doesn't exist
     */
    public static User getUserByEmail(final String email) throws UserNotFoundException {
        try {
            return instance
                    .getUsers()
                    .parallelStream()
                    .filter(user -> user.getEmail().equals(email))
                    .toList()
                    .getFirst();
        } catch (NoSuchElementException e) {
            throw new UserNotFoundException("No user found using email: " + email);
        }
    }

    /**
     * Finds an user
     * @param accountIBAN the IBAN of the account owned by the user
     * @return the requested user
     * @throws UserNotFoundException if no user owns an account with the given IBAN
     */
    public static User getUserByIBAN(final String accountIBAN) throws UserNotFoundException {
        // Might get changed if I remove the hashmap
        User u = instance.getAccountMap().get(accountIBAN);
        if (u == null) {
            throw new UserNotFoundException("No user found using IBAN: " + accountIBAN);
        }

        return u;
    }

    /**
     * Finds an account
     * @param accountIBAN the account's IBAN to search for
     * @return the requested account
     * @throws UserNotFoundException if no user owns an account with the requested IBAN
     */
    public static Account getAccount(
            final String accountIBAN
    ) throws UserNotFoundException, OwnershipException {
        return getUserByIBAN(accountIBAN).getAccount(accountIBAN);
    }

    /**
     * Finds a card
     * @param cardNumber the number of the card to search
     * @return the requested card
     * @throws OwnershipException if no user owns a card matching the given number
     */
    public static Card getCard(final String cardNumber) throws OwnershipException {
        AtomicReference<Card> targetCard = new AtomicReference<>();
        instance.getUsers().parallelStream().forEach(
                u -> u.getAccounts().parallelStream().forEach(
                        account -> account.getCards().parallelStream().forEach(
                                card -> {
                                    if (card.getCardNumber().equals(cardNumber)) {
                                        targetCard.set(card);
                                    }
                                }
                        )
                )
        );
        if (targetCard.get() == null) {
            throw new OwnershipException("No card found: " + cardNumber);
        }

        return targetCard.get();
    }

    /**
     * Finds an account
     * @param alias the associated name to search for
     * @return the requested Account
     * @throws OwnershipException if no account is associated to the given alias
     */
    public static Account getByAlias(final String alias) throws OwnershipException {
        if (instance.getAliasMap().get(alias) == null) {
            throw new OwnershipException("There's no account with the alias: " + alias);
        }

        try {
            return getAccount(instance.getAliasMap().get(alias));
        } catch (UserNotFoundException | OwnershipException e) {
            throw new OwnershipException(e.getMessage() + " [alias: " + alias + "]");
        }
    }

}
