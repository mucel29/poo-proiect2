package org.poo.system.user;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import org.poo.io.IOUtils;
import org.poo.io.StateWriter;
import org.poo.system.Transaction;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.utils.NodeConvertable;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
public class User implements NodeConvertable {
    private final String firstName;
    private final String lastName;
    private final String email;

    private final List<Account> accounts = new ArrayList<>();

    public User(
            final String firstName,
            final String lastName,
            final String email
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
    }

    /**
     * Reads a user
     * @param node the JSON node representing the user
     * @return the deserialized node as an {@code User}
     * @throws BankingInputException if the node could not be deserialized to an
     * {@code User} instance
     */
    public static User read(final JsonNode node) throws BankingInputException {
        if (!node.isObject()) {
            throw new BankingInputException("User node is not an object");
        }

        String firstNameField = IOUtils.readStringChecked(node, "firstName");
        String lastNameField = IOUtils.readStringChecked(node, "lastName");
        String emailField = IOUtils.readStringChecked(node, "email");

        return new User(firstNameField, lastNameField, emailField);
    }

    /**
     * Reads an array of users
     * @param node the node containing the users
     * @return a List of deserialized {@code User} instances
     * @throws BankingInputException if the given node is not an array
     */
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
                // If the user could not be read,
                // continue reading the other users,
                // it's not a critical failing point
                e.printStackTrace();
            }
        }

        return userList;
    }

    /**
     * Finds an account owned by this user
     * @param account the account number to search for
     * @return the account instance associated with the IBAN
     * @throws OwnershipException if the queried account is not owned by this user
     */
    public Account getAccount(final String account) throws OwnershipException {
        try {
            return this.accounts
                    .stream()
                    .filter(a -> a.getAccountIBAN().equals(account))
                    .toList()
                    .getFirst();
        } catch (NoSuchElementException e) {
            throw new OwnershipException(
                    "User `"
                            + this.email
                            + "` does not own an account with the IBAN `"
                            + account
                            + "`"
            );
        }
    }

    /**
     * Aggregates all the accounts' transactions
     * @return a list containing all the transactions belonging to this user
     */
    public List<Transaction> getTransactions() {
        return accounts
                .stream()
                .flatMap(account -> account.getTransactions().stream())
                .sorted(Transaction::compareTo)
                .collect(Collectors.toList());

    }

    /**
     * @param obj the other object to compare with
     * @return whether this instance and obj are equal
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof User other) {
            return firstName.equals(other.firstName)
                    && lastName.equals(other.lastName)
                    && email.equals(other.email)
                    && accounts.equals(other.accounts);
        }

        return false;
    }

    /**
     * @return the user's hash
     */
    @Override
    public int hashCode() {
        return Objects.hash(
                firstName,
                lastName,
                email,
                accounts
        );
    }

    /**
     * @return the JSON representation of the {@code User}
     */
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
