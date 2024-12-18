package org.poo.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exchange.ComposedExchangeProvider;
import org.poo.system.exchange.Exchange;
import org.poo.system.exchange.ExchangeProvider;
import org.poo.system.storage.MappedStorage;
import org.poo.system.storage.StorageProvider;
import org.poo.system.user.User;
import org.poo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@Getter
public final class BankingSystem {

    private final List<Command> commands = new ArrayList<>();

    private ExchangeProvider exchangeProvider;
    private StorageProvider storageProvider;

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
        commands.clear();
        instance.exchangeProvider = new ComposedExchangeProvider();
        instance.storageProvider = new MappedStorage();

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
        User.readArray(usersNode)
                .forEach(user -> instance.storageProvider.registerUser(user));

        // Read exchange rates
        JsonNode exchangeNode = root.get("exchangeRates");
        if (exchangeNode == null) {
            throw new BankingInputException("No exchange rates found");
        }

        getExchangeProvider().registerExchanges(
                Exchange.readArray(exchangeNode)
        );
        getExchangeProvider().printRates();



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

    public static ExchangeProvider getExchangeProvider() {
        return instance.exchangeProvider;
    }

    public static StorageProvider getStorageProvider() {
        return instance.storageProvider;
    }

}
