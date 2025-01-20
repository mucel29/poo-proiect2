package org.poo.system.user;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import lombok.Setter;
import org.poo.io.IOUtils;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.payments.PaymentObserver;
import org.poo.system.payments.PaymentOrder;
import org.poo.system.payments.PendingPayment;
import org.poo.system.user.plan.ServicePlan;
import org.poo.system.user.plan.ServicePlanFactory;
import org.poo.utils.NodeConvertable;
import org.poo.utils.Pair;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Getter
public class User implements NodeConvertable, PaymentObserver {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String occupation;
    private final LocalDate birthDate;

    private final List<Account> accounts = new ArrayList<>();
    private final List<Pair<PendingPayment, Integer>> pendingPayments = new ArrayList<>();

    @Setter
    private ServicePlan servicePlan;

    public User(
            final String firstName,
            final String lastName,
            final String email,
            final String occupation,
            final LocalDate birthDate
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.occupation = occupation;
        this.birthDate = birthDate;
        this.servicePlan = ServicePlanFactory.getPlan(
                this,
                occupation.equals("student")
                        ? ServicePlan.Tier.STUDENT
                        : ServicePlan.Tier.STANDARD
        );
    }

    /**
     * Reads a user
     * @param node the JSON node representing the user
     * @return the deserialized node as an {@code User}
     * @throws InputException if the node could not be deserialized to an
     * {@code User} instance
     */
    public static User read(final JsonNode node) throws InputException {
        if (!node.isObject()) {
            throw new InputException("User node is not an object");
        }

        String firstNameField = IOUtils.readStringChecked(node, "firstName");
        String lastNameField = IOUtils.readStringChecked(node, "lastName");
        String emailField = IOUtils.readStringChecked(node, "email");
        String occupationField = IOUtils.readStringChecked(node, "occupation");
        LocalDate birthDateField = IOUtils.readDateChecked(node, "birthDate");

        return new User(
                firstNameField,
                lastNameField,
                emailField,
                occupationField,
                birthDateField
        );
    }

    /**
     * Reads an array of users
     * @param node the node containing the users
     * @return a List of deserialized {@code User} instances
     * @throws InputException if the given node is not an array
     */
    public static List<User> readArray(final JsonNode node) throws InputException {
        if (node == null) {
            throw new InputException("No users found");
        }

        if (!node.isArray()) {
            throw new InputException("User list is not an array");
        }

        ArrayNode users = (ArrayNode) node;
        List<User> userList = new ArrayList<>();

        for (JsonNode user : users) {
            try {
                userList.add(read(user));
            } catch (InputException e) {
                // If the user could not be read,
                // continue reading the other users,
                // it's not a critical failing point
                if (BankingSystem.VERBOSE_LOGGING) {
                    System.err.println(e.getDetailedMessage());
                }
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
                .filter(account -> account.owner.equals(this))
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
     * Gets the {@code User}'s current age
     * @return the {@code User}'s age
     */
    public Period getAge() {
        return Period.between(birthDate, BankingSystem.getInstance().getCurrentDate());
    }

    /**
     * @return the user's names concatenated
     */
    public String getUsername() {
        return lastName + " " + firstName;
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
            if (
                    account.getAccountType() == Account.Type.BUSINESS
                            && !account.getOwner().equals(this)
            ) {
                continue;
            }
            accountsNode.add(account.toNode());
        }

        return root;
    }

    private Pair<PendingPayment, Integer> getOrder(
            final PendingPayment payment
    ) {
        Optional<Pair<PendingPayment, Integer>> entry =
                pendingPayments
                        .stream()
                        .filter(p -> p.first().equals(payment))
                        .findFirst();

        return entry.orElse(null);
    }

    /**
     * Registers the pending payment to the observer
     *
     * @param payment the payment to register
     */
    @Override
    public void register(final PendingPayment payment) {
        var pending = getOrder(payment);

        if (pending == null) {
            pending = new Pair<>(payment, 1);
            pendingPayments.add(pending);
            return;
        }

        pendingPayments.remove(pending);
        pendingPayments.add(new Pair<>(pending.first(), pending.second() + 1));
    }

    /**
     * Notifies the observer of a payment to be made
     *
     * @param order the payment order to execute
     */
    @Override
    public void notify(final PaymentOrder order) {
        var entry = getOrder(order.payment());
        if (entry == null) {
            return;
        }

        order.account().setFunds(order.account().getFunds().sub(order.amount()));
        order.account().getTransactions().add(order.transaction());

        // Remove one account from the payment
        // Don't add it back if all accounts are finished
        pendingPayments.remove(entry);
        if (entry.second() > 1) {
            pendingPayments.add(new Pair<>(order.payment(), entry.second() - 1));
        }
    }

    /**
     * @param account the account to check
     * @return whether the observer owns the given account
     */
    @Override
    public boolean owns(final Account account) {
        return accounts.contains(account);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public String toString() {
        return email;
    }
}
