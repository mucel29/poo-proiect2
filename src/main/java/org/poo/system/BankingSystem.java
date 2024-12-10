package org.poo.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.ExchangeException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exchange.Pair;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Getter @Setter
public class BankingSystem {


    private final List<User> users = new ArrayList<>();
    private final List<Command> commands = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<Exchange> exchanges = new ArrayList<>();

    private final Map<String, User> accountMap = new HashMap<>();
    private final Map<String, Double> commerciantSpending = new HashMap<>();

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
        commerciantSpending.clear();
        exchanges.clear();

        Utils.resetRandom();

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
        instance.getExchanges().addAll(Exchange.readArray(exchangeNode));
        // Add composed exchanges (through various currencies)
        Exchange.computeComposedRates(instance.getExchanges());

        // Read commands
        JsonNode commandsNode = root.get("commands");
        if (commandsNode == null) {
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
            return instance.getUsers().parallelStream().filter(user -> user.getEmail().equals(email)).toList().getFirst();
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

    public static Card getCard(final String cardNumber) throws OwnershipException {
        AtomicReference<Card> targetCard = new AtomicReference<>();
        instance.getUsers().parallelStream().forEach(u -> {
            u.getAccounts().parallelStream().forEach(account -> {
                account.getCards().parallelStream().forEach(card -> {
                    if (card.getCardNumber().equals(cardNumber)) {
                        targetCard.set(card);
                    }
                });
            });
        });
        if (targetCard.get() == null) {
            throw new OwnershipException("No card found: " + cardNumber);
        }

        return targetCard.get();
    }

    public static double getExchangeRate(Exchange.Currency from, Exchange.Currency to) throws ExchangeException {
        if (from.equals(to)) {
            return 1.0;
        }
        try {
            return instance.getExchanges().parallelStream().filter(ex -> ex.getFrom().equals(from) && ex.getTo().equals(to)).toList().getFirst().getRate();
        } catch (NoSuchElementException e) {
            throw new ExchangeException("No rate found for " + from + " -> " + to);
        }
    }

    public static void addCommerciantPayment(final String commerciant, final double payment) {
        if (!instance.getCommerciantSpending().containsKey(commerciant)) {
            instance.getCommerciantSpending().put(commerciant, payment);
            return;
        }
        instance.getCommerciantSpending().put(commerciant, instance.getCommerciantSpending().get(commerciant) + payment);
    }

}
