package org.poo.system.storage;

import org.poo.system.exceptions.AliasException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.user.User;
import org.poo.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@code StorageProvider} implementation that stores all users,
 * accounts, cards and aliases inside maps for faster access
 */
public final class MappedStorage implements StorageProvider {

    private final List<User> userList = new ArrayList<>();

    private final Map<String, User> users = new HashMap<>();
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, Card> cards = new HashMap<>();

    private final Map<String, Account> aliases = new HashMap<>();

    private boolean isRegistered(final User user) {
        return userList.contains(user);
    }

    private boolean isRegistered(final Account account) {
        return accounts.containsValue(account);
    }

    private boolean isRegistered(final Card card) {
        return cards.containsValue(card);
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

        users.put(user.getEmail(), user);
        userList.add(user);
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

        account.getOwner().getAccounts().add(account);
        accounts.put(account.getAccountIBAN(), account);
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

        card.getAccount().getCards().add(card);
        cards.put(card.getCardNumber(), card);
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

        aliases.remove(account.getAlias());
        account.setAlias(alias);
        aliases.put(alias, account);
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
        account.getCards().forEach(card -> cards.remove(card.getCardNumber()));
        accounts.remove(account.getAccountIBAN());
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
        cards.remove(card.getCardNumber());
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
        if (!users.containsKey(email)) {
            throw new UserNotFoundException("No user found using email: " + email);
        }

        return users.get(email);
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
        if (!accounts.containsKey(iban)) {
            throw new UserNotFoundException("No user found using IBAN: " + iban);
        }

        return accounts.get(iban).getOwner();
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
        if (!accounts.containsKey(iban)) {
            throw new OwnershipException("No account found using IBAN: " + iban);
        }

        return accounts.get(iban);
    }

    /**
     * Finds an account
     *
     * @param alias the associated name to search for
     * @return the requested Account
     * @throws OwnershipException if no account is associated to the given alias
     * @throws AliasException     if the given alias is invalid
     */
    @Override
    public Account getAccountByAlias(final String alias) throws OwnershipException, AliasException {
        if (Utils.verifyIBAN(alias)) {
            throw new AliasException("Unsupported alias format: " + alias);
        }

        if (!aliases.containsKey(alias)) {
            throw new OwnershipException("No account found using alias: " + alias);
        }

        return aliases.get(alias);
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
        if (!cards.containsKey(cardNumber)) {
            throw new OwnershipException("No card found: " + cardNumber);
        }

        return cards.get(cardNumber);
    }

    /**
     * Retrieves all the users
     *
     * @return a list of all the users stored inside
     */
    @Override
    public List<User> getUsers() {
        return userList;
    }

    /**
     * Retrieves all the accounts
     *
     * @return a list of all the accounts stored inside
     */
    @Override
    public List<Account> getAccounts() {
        return accounts.values().stream().toList();
    }

    /**
     * Retrieves all the cards
     *
     * @return a list of all the cards stored inside
     */
    @Override
    public List<Card> getCards() {
        return cards.values().stream().toList();
    }
}
