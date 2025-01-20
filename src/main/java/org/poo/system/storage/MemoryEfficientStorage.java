package org.poo.system.storage;

import org.poo.system.exceptions.AliasException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.commerce.Commerciant;
import org.poo.system.user.User;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@code StorageProvider} implementation that stores only the users.
 * </br>
 * All other objects are accessed through the hierarchy, starting from the user
 */
public final class MemoryEfficientStorage implements StorageProvider {

    private final List<User> users = new ArrayList<>();
    private final List<Commerciant> commerciants = new ArrayList<>();

    private boolean isRegistered(final User user) {
        return users.contains(user);
    }

    private boolean isRegistered(final Commerciant commerciant) {
        return commerciants.contains(commerciant);
    }

    private boolean isRegistered(final Account account) {
        Optional<User> registered = users.stream().filter(
                user -> user.getAccounts().contains(account)
        ).findFirst();

        return registered.isPresent() && !account.isUnauthorized(registered.get());
    }

    private boolean isRegistered(final Card card) {
        Optional<Account> registered = getAccounts().stream().filter(
                account -> account.getCards().contains(card)
        ).findFirst();

        return registered.isPresent() && registered.get().equals(card.getAccount());
    }

    /**
     * Registers an {@code User} into the storage
     *
     * @param user the {@code User} to register
     * @throws StorageException if the user already exists
     */
    @Override
    public void registerUser(final User user) throws StorageException {
        if (isRegistered(user)) {
            throw new StorageException("User " + user.getEmail() + " is already registered");
        }
        users.add(user);
    }

    /**
     * Registers an {@code Commerciant} into the storage
     *
     * @param commerciant the {@code Commerciant} to register
     * @throws StorageException if the commerciant already exists
     */
    @Override
    public void registerCommerciant(final Commerciant commerciant) throws StorageException {
        if (isRegistered(commerciant)) {
            throw new StorageException(
                    "Commerciant "
                            + commerciant.getName()
                            + " is already registered"
            );
        }

        commerciants.add(commerciant);
    }

    /**
     * Registers an {@code Account} into the storage
     *
     * @param account the {@code Account} to register
     * @throws StorageException <ul>
     *                          <li>The account owner is not a registered user</li>
     *                          <li>The account is already registered to another user</li>
     *                          </ul>
     */
    @Override
    public void registerAccount(final Account account) throws StorageException {
        if (!isRegistered(account.getOwner())) {
            throw new StorageException(
                    "Account "
                            + account.getAccountIBAN()
                            + " owner "
                            + account.getOwner().getEmail()
                            + " is not registered"
            );
        }

        if (isRegistered(account)) {
            throw new StorageException(
                    "Account "
                            + account.getAccountIBAN()
                            + " is already registered to another user"
            );
        }

        // If there isn't a duplicate, the account is already registered
        account.getOwner().getAccounts().add(account);
    }

    /**
     * Registers an {@code Card} into the storage
     *
     * @param card the {@code Card} to register
     * @throws StorageException <ul>
     *                          <li>The card's owner is not a registered user</li>
     *                          <li>The card's associated account is not registered</li>
     *                          <li>The card is already associated to another account</li>
     *                          </ul>
     */
    @Override
    public void registerCard(final Card card) throws StorageException {
        if (!isRegistered(card.getAccount().getOwner())) {
            throw new StorageException(
                    "Card "
                            + card.getCardNumber()
                            + " owner "
                            + card.getAccount().getOwner().getEmail()
                            + " is not registered"
            );
        }

        if (!isRegistered(card.getAccount())) {
            throw new StorageException(
                    "Card "
                        + card.getCardNumber()
                        + " associated to account "
                        + card.getAccount().getAccountIBAN()
                        + " is not registered"
            );
        }

        if (isRegistered(card)) {
            throw new StorageException(
                    "Card "
                        + card.getCardNumber()
                        + " is already associated to another account"
            );
        }

        // If there isn't a duplicate, the account is already registered
        card.getAccount().getCards().add(card);
    }

    /**
     * Associates an alias an {@code Account}.
     * </br>
     * Overrides the alias of the account if it already has one.
     *
     * @param account the {@code Account} to associate the alias to
     * @param alias   the alias to use for the given {@code Account}
     * @throws StorageException <ul>
     *                          <li>The account owner is not a registered user</li>
     *                          <li>The account is already registered to another user</li>
     *                          </ul>
     * @throws AliasException   if the alias is invalid
     */
    @Override
    public void registerAlias(
            final Account account,
            final String alias
    ) throws StorageException, AliasException {
        if (Utils.verifyIBAN(alias)) {
            throw new AliasException("Unsupported alias format: " + alias);
        }

        if (!isRegistered(account.getOwner())) {
            throw new StorageException(
                    "Account "
                            + account.getAccountIBAN()
                            + " owner "
                            + account.getOwner().getEmail()
                            + " is not registered"
            );
        }

        if (!isRegistered(account)) {
            throw new StorageException(
                    "Account "
                            + account.getAccountIBAN()
                            + " is not registered"
            );
        }

        // Set alias if it wasn't set already. Nothing else to do here
        account.setAlias(alias);
    }

    /**
     * Removes an {@code Account} from the storage
     *
     * @param account the {@code Account} to remove
     * @throws StorageException <ul>
     *                          <li>The account owner is not a registered user</li>
     *                          <li>The account is not registered</li>
     *                          </ul>
     */
    @Override
    public void removeAccount(final Account account) throws StorageException {
        if (!isRegistered(account.getOwner())) {
            throw new StorageException(
                    "Account "
                        + account.getAccountIBAN()
                        + " owner "
                        + account.getOwner().getEmail()
                        + " is not registered"
            );
        }

        if (!isRegistered(account)) {
            throw new StorageException(
                    "Account "
                        + account.getAccountIBAN()
                        + " is not registered"
            );
        }

        account.getOwner().getAccounts().remove(account);
    }

    /**
     * Removes an {@code Card} from the storage
     *
     * @param card the {@code Card} to remove
     * @throws StorageException <ul>
     *                          <li>The card's owner is not a registered user</li>
     *                          <li>The card's associated account is not registered</li>
     *                          <li>The card is not registered</li>
     *                          </ul>
     */
    @Override
    public void removeCard(final Card card) throws StorageException {
        if (!isRegistered(card.getAccount().getOwner())) {
            throw new StorageException(
                    "Card "
                        + card.getCardNumber()
                        + " owner "
                        + card.getAccount().getOwner().getEmail()
                        + " is not registered"
            );
        }

        if (!isRegistered(card.getAccount())) {
            throw new StorageException(
                    "Card "
                        + card.getCardNumber()
                        + " associated to account "
                        + card.getAccount().getAccountIBAN()
                        + " is not registered"
            );
        }

        if (!isRegistered(card)) {
            throw new StorageException(
                    "Card "
                    + card.getCardNumber()
                    + " is not registered"
            );
        }

        card.getAccount().getCards().remove(card);
    }

    /**
     * Finds an user
     *
     * @param email the email associated with the user
     * @return the requested user
     * @throws UserNotFoundException if the user doesn't exist
     */
    @Override
    public User getUserByEmail(final String email) throws UserNotFoundException {
        Optional<User> user = users.stream().filter(u -> u.getEmail().equals(email)).findFirst();
        if (user.isEmpty()) {
            throw new UserNotFoundException("No user found using email: " + email);
        }

        return user.get();
    }

    /**
     * Finds an user
     *
     * @param iban the IBAN of the account owned by the user
     * @return the requested user
     * @throws UserNotFoundException if no user owns an account with the given IBAN
     */
    @Override
    public User getUserByIban(final String iban) throws UserNotFoundException {
        Optional<User> user =
                users.stream().filter(
                    u -> u.getAccounts().stream().anyMatch(
                            a -> a.getAccountIBAN().equals(iban)
                    )
                ).findFirst();

        if (user.isEmpty()) {
            throw new UserNotFoundException("No user found using IBAN: " + iban);
        }

        return user.get();
    }

    /**
     * Finds a commerciant
     *
     * @param iban the IBAN of the commerciant
     * @return the requested commerciant
     * @throws UserNotFoundException if no commerciant exists with the given IBAN
     */
    @Override
    public Commerciant getCommerciantByIban(final String iban) throws UserNotFoundException {
        Optional<Commerciant> commerciant = commerciants.stream().filter(
                comm -> comm.getAccountIBAN().equals(iban)
        ).findFirst();

        if (commerciant.isEmpty()) {
            throw new UserNotFoundException("No commerciant found using IBAN: " + iban);
        }

        return commerciant.get();
    }

    /**
     * Finds a commerciant
     *
     * @param name the IBAN of the commerciant
     * @return the requested commerciant
     * @throws UserNotFoundException if no commerciant exists with the given name
     */
    @Override
    public Commerciant getCommerciantByName(final String name) throws UserNotFoundException {
        Optional<Commerciant> commerciant = commerciants.stream().filter(
                comm -> comm.getName().equals(name)
        ).findFirst();

        if (commerciant.isEmpty()) {
            throw new UserNotFoundException("No commerciant found using name: " + name);
        }

        return commerciant.get();
    }

    /**
     * Retrieves all registered commerciants
     *
     * @return an immutable list of commerciants
     */
    @Override
    public List<Commerciant> getCommerciants() {
        return List.of(commerciants.toArray(new Commerciant[0]));
    }

    /**
     * Finds an account
     *
     * @param iban the account's IBAN to search for
     * @return the requested account
     * @throws OwnershipException if no user owns an account with the requested IBAN
     */
    @Override
    public Account getAccountByIban(final String iban) throws OwnershipException {
        Optional<Account> account = getAccounts()
                .stream()
                .filter(a -> a.getAccountIBAN().equals(iban))
                .findFirst();

        if (account.isEmpty()) {
            throw new OwnershipException("No account found using IBAN: " + iban);
        }

        return account.get();
    }

    /**
     * Finds an account
     *
     * @param alias the associated name to search for
     * @return the requested Account
     * @throws OwnershipException if no account is associated to the given alias
     * @throws AliasException if the given alias is invalid
     */
    @Override
    public Account getAccountByAlias(final String alias) throws OwnershipException, AliasException {
        if (Utils.verifyIBAN(alias)) {
            throw new AliasException("Unsupported alias format: " + alias);
        }

        Optional<Account> account = getAccounts().stream().filter(
                a -> !a.getAlias().isEmpty() && a.getAlias().equals(alias)
        ).findFirst();

        if (account.isEmpty()) {
            throw new OwnershipException("No account found using alias: " + alias);
        }

        return account.get();
    }

    /**
     * Finds a card
     *
     * @param cardNumber the number of the card to search
     * @return the requested card
     * @throws OwnershipException if no user owns a card matching the given number
     */
    @Override
    public Card getCard(final String cardNumber) throws OwnershipException {
        Optional<Card> card = getCards().stream().filter(
                c -> c.getCardNumber().equals(cardNumber)
        ).findFirst();

        if (card.isEmpty()) {
            throw new OwnershipException("No card found: " + cardNumber);
        }

        return card.get();
    }

    /**
     * Retrieves all the users
     *
     * @return a list of all the users stored inside
     */
    @Override
    public List<User> getUsers() {
        return users;
    }

    /**
     * Retrieves all the accounts
     * @return a list of all the accounts stored inside
     */
    @Override
    public List<Account> getAccounts() {
        return users
                .stream()
                .map(User::getAccounts)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all the cards
     *
     * @return a list of all the cards stored inside
     */
    @Override
    public List<Card> getCards() {
        return users.stream().map(
                u -> u.getAccounts().stream().map(
                        Account::getCards
                ).flatMap(Collection::stream).collect(Collectors.toList())
        ).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
