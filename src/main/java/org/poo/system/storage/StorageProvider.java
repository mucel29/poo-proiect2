package org.poo.system.storage;

import org.poo.system.exceptions.AliasException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.Account;
import org.poo.system.user.Card;
import org.poo.system.commerce.Commerciant;
import org.poo.system.user.User;

import java.util.List;

public interface StorageProvider {

    /**
     * Registers an {@code User} into the storage
     *
     * @param user the {@code User} to register
     * @throws StorageException if the user already exists
     */
    void registerUser(User user) throws StorageException;

    /**
     * Registers an {@code Commerciant} into the storage
     *
     * @param commerciant the {@code Commerciant} to register
     * @throws StorageException if the commerciant already exists
     */
    void registerCommerciant(Commerciant commerciant) throws StorageException;

    /**
     * Registers an {@code Account} into the storage
     *
     * @param account the {@code Account} to register
     * @throws StorageException <ul>
     *                          <li>The account owner is not a registered user</li>
     *                          <li>The account is already registered to another user</li>
     *                          </ul>
     */
    void registerAccount(Account account) throws StorageException;

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
    void registerCard(Card card) throws StorageException;

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
    void registerAlias(
            Account account,
            String alias
    ) throws StorageException, AliasException;

    /**
     * Removes an {@code Account} from the storage
     *
     * @param account the {@code Account} to remove
     * @throws StorageException <ul>
     *                          <li>The account owner is not a registered user</li>
     *                          <li>The account is not registered</li>
     *                          </ul>
     */
    void removeAccount(Account account) throws StorageException;

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
    void removeCard(Card card) throws StorageException;

    /**
     * Finds an user
     *
     * @param email the email associated with the user
     * @return the requested user
     * @throws UserNotFoundException if the user doesn't exist
     */
    User getUserByEmail(String email) throws UserNotFoundException;

    /**
     * Finds an user
     *
     * @param iban the IBAN of the account owned by the user
     * @return the requested user
     * @throws UserNotFoundException if no user owns an account with the given IBAN
     */
    User getUserByIban(String iban) throws UserNotFoundException;

    /**
     * Finds a commerciant
     *
     * @param iban the IBAN of the commerciant
     * @return the requested commerciant
     * @throws UserNotFoundException if no commerciant exists with the given IBAN
     */
    Commerciant getCommerciantByIban(String iban) throws UserNotFoundException;

    /**
     * Finds a commerciant
     *
     * @param name the IBAN of the commerciant
     * @return the requested commerciant
     * @throws UserNotFoundException if no commerciant exists with the given name
     */
    Commerciant getCommerciantByName(String name) throws UserNotFoundException;


    /**
     * Finds an account
     *
     * @param iban the account's IBAN to search for
     * @return the requested account
     * @throws OwnershipException if no user owns an account with the requested IBAN
     */
    Account getAccountByIban(String iban) throws OwnershipException;

    /**
     * Finds an account
     *
     * @param alias the associated name to search for
     * @return the requested Account
     * @throws OwnershipException if no account is associated to the given alias
     * @throws AliasException if the given alias is invalid
     */
    Account getAccountByAlias(String alias) throws OwnershipException, AliasException;

    /**
     * Finds a card
     *
     * @param cardNumber the number of the card to search
     * @return the requested card
     * @throws OwnershipException if no user owns a card matching the given number
     */
    Card getCard(String cardNumber) throws OwnershipException;

    /**
     * Retrieves all the users
     * @return a list of all the users stored inside
     */
    List<User> getUsers();

    /**
     * Retrieves all the accounts
     * @return a list of all the accounts stored inside
     */
    List<Account> getAccounts();

    /**
     * Retrieves all the cards
     * @return a list of all the cards stored inside
     */
    List<Card> getCards();

}
