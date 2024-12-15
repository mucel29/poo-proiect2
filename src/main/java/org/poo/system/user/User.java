package org.poo.system.user;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.StateWriter;
import org.poo.system.Transaction;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.utils.NodeConvertable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Getter
@Setter
public class User implements NodeConvertable {
    private String firstName;
    private String lastName;
    private String email;

    private final List<Account> accounts = new ArrayList<>();

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    public static User read(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("User node is not an object");
        }

        JsonNode firstNameField = node.get("firstName");
        JsonNode lastNameField = node.get("lastName");
        JsonNode emailField = node.get("email");

        if (firstNameField.asText().isEmpty() || lastNameField.asText().isEmpty() || emailField.asText().isEmpty()) {
            throw new BankingInputException("The user is missing a field:\n" + node.toPrettyString());
        }

        return new User(firstNameField.asText(), lastNameField.asText(), emailField.asText());
    }

    public static List<User> readArray(final JsonNode node) throws BankingInputException {
        if (!node.isArray()) {
            throw new BankingInputException("User list is not an array");
        }

        ArrayNode users = (ArrayNode) node;
        List<User> userList = new ArrayList<>();

        for (JsonNode user : users) {
            try {
                userList.add(read(user));
            } catch (BankingInputException e) {
                // If the user could not be read, continue reading the other users, it's not a critical failing point
                e.printStackTrace();
            }
        }

        return userList;
    }

    public Account getAccount(String IBAN) throws OwnershipException {
        try {
            return this.accounts.stream().filter(account -> account.getIBAN().equals(IBAN)).toList().getFirst();
        } catch (NoSuchElementException e) {
            throw new OwnershipException("User `" + this.email + "` does not own an account with the IBAN `" + IBAN + "`");
        }
    }

    public List<Transaction> getTransactions() {
        return accounts
                .stream()
                .flatMap(account -> account.getTransactions().stream())
                .sorted(Transaction::compareTo)
                .collect(Collectors.toList());

    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof User other) {
            return firstName.equals(other.firstName) && lastName.equals(other.lastName) && email.equals(other.email) && accounts.equals(other.accounts);
        }

        return false;
    }

    @Override
    public ObjectNode toNode() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();
        root.put("firstName", firstName);
        root.put("lastName", lastName);
        root.put("email", email);

        ArrayNode accountsNode = root.putArray("accounts");
        for (Account account : accounts) {
            accountsNode.add(account.toNode());
        }

        return root;
    }
}
