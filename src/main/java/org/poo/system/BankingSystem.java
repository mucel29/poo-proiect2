package org.poo.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.poo.system.command.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.User;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Getter @Setter
public class BankingSystem {

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


    private final List<User> users = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();
    private final HashMap<String, User> accountMap = new HashMap<>();
    private final List<Transaction> transactions = new ArrayList<>();

    private BankingSystem() {};

    private static BankingSystem instance;

    public static BankingSystem getInstance() throws IllegalStateException {
        if (instance == null) {
            throw new IllegalStateException("System not initialized");
        }

        return instance;
    }

    private void reset() {
        users.clear();
        commands.clear();
        accountMap.clear();
        transactions.clear();
    }

    public static void init(final File file) throws IOException {
        if (instance == null) {
            instance = new BankingSystem();
        }

        instance.reset();

        String jsonString = Files.readString(file.toPath());
        JsonNode root = new ObjectMapper().readTree(jsonString);

        // Read data from root

        // Read users
        JsonNode usersNode = root.get("users");
        if (usersNode.isNull()) {
            throw new BankingInputException("No users found");
        }
        instance.getUsers().addAll(User.readArray(usersNode));

        // Todo: read exchange rates

        // Read commands
        JsonNode commandsNode = root.get("commands");
        if (commandsNode.isNull()) {
            throw new BankingInputException("No commands found");
        }
        instance.getCommands().addAll(Command.readArray(commandsNode));


    }

    public static void run() {
        for (Command command : instance.getCommands()) {
            command.execute();
        }
    }

    public static User getUserByEmail(final String email) throws UserNotFoundException {
        try {
            return instance.getUsers().stream().filter(user -> user.getEmail().equals(email)).toList().getFirst();
        } catch (NoSuchElementException e) {
            throw new UserNotFoundException("No user found using email: " + email);
        }
    }

    public static User getUserByIBAN(final String IBAN) throws UserNotFoundException {
        // Might get changed if I remove the hashmap
        User u = instance.getAccountMap().get(IBAN);
        if (u == null) {
            throw new UserNotFoundException("No user found using IBAN: " + IBAN);
        }

        return u;
    }

    public static Account getAccount(final String IBAN) throws UserNotFoundException, OwnershipException {
        return getUserByIBAN(IBAN).getAccount(IBAN);
    }

}
