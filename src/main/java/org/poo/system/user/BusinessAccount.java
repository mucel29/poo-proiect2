package org.poo.system.user;

import lombok.Getter;
import lombok.Setter;
import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.commerce.CommerciantSpending;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exchange.Amount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Getter @Setter
public class BusinessAccount extends Account {

    public enum Role {
        MANAGER,
        EMPLOYEE;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        /**
         * Converts a String to an {@code BusinessAccount.Role}
         * @param label the string to convert
         * @return the corresponding {@code BusinessAccount.Role}
         * @throws InputException if the label can't be converted to an {@code BusinessAccount.Role}
         */
        public static BusinessAccount.Role fromString(final String label) throws InputException {
            try {
                return Arrays
                        .stream(BusinessAccount.Role.values())
                        .filter(role -> role.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown account role: " + label);
            }
        }
    }

    private static final Amount INITIAL_LIMIT = new Amount(500, "RON");

    private final List<AssociateData> associateDataList = new ArrayList<>();
    private final List<CommerciantSpending> commerciantSpendingList = new ArrayList<>();

    public BusinessAccount(
            final User owner,
            final String accountIBAN,
            final String currency
    ) {
        super(owner, accountIBAN, currency, Type.BUSINESS);
        spendingLimit = INITIAL_LIMIT.to(currency);
        depositLimit = INITIAL_LIMIT.to(currency);
    }

    /**
     * Adds a new associate to the account
     *
     * @param associate the associate
     * @param role a role
     * @throws OperationException if the user is already associated
     */
    public void addAssociate(
            final User associate,
            final Role role
    ) throws OperationException {
        if (owner.equals(associate)) {
            throw new OperationException(
                   "Associate is the owner"
            );
        }

        // Retrieve the associate data
        AssociateData data;
        try {
            data = getAssociateData(associate);
        } catch (OwnershipException e) {
            // The user is not an associate, which is good
            // Add it
            associateDataList.add(new AssociateData(
                    associate,
                    role,
                    Amount.zero(funds.currency()),
                    Amount.zero(funds.currency())
            ));
            // Give access to the associate to the account
            associate.getAccounts().add(this);

            BankingSystem.log(
                    "Added "
                            + associate.getUsername()
                            + " as "
                            + role
                            + " to "
                            + accountIBAN
            );

        }

        // Associate was found, can't have multiple roles
        throw new OperationException(
                "Associate "
                        + associate.getEmail()
                        + " already has a role"
        );



    }

    /**
     * @param associate the associate
     * @return the data regarding the given associate
     * @throws OwnershipException if the given user is not authorized for this account
     */
    public AssociateData getAssociateData(
            final User associate
    ) throws OwnershipException {
        Optional<AssociateData> associateData =
                associateDataList
                        .stream()
                        .filter(ad -> ad.associate().equals(associate))
                        .findFirst();

        if (associateData.isEmpty()) {
            throw new OwnershipException(
                    "Associate "
                            + associate.getEmail()
                            + " not found"
            );
        }

        return associateData.get();
    }

    private void updateDeposit(
            final User associate,
            final Amount deposited
    ) throws OwnershipException {
        AssociateData data = getAssociateData(associate);

        AssociateData newData = new AssociateData(
                associate,
                data.role(),
                data.spent(),
                deposited
        );

        BankingSystem.log(
                "Updated deposit for "
                        + associate.getUsername()
                        + ": "
                        + data.deposited()
                        + " -> "
                        + deposited
        );

        int oldIndex = associateDataList.indexOf(data);
        associateDataList.remove(data);
        associateDataList.add(oldIndex, newData);
    }

    private void updateSpending(
            final User associate,
            final Amount spending
    ) throws OwnershipException {
        AssociateData data = getAssociateData(associate);

        AssociateData newData = new AssociateData(
                associate,
                data.role(),
                spending,
                data.deposited()
        );

        BankingSystem.log(
                "Updated spending for "
                        + associate.getUsername()
                        + ": "
                        + data.spent()
                        + " -> "
                        + spending
        );

        int oldIndex = associateDataList.indexOf(data);
        associateDataList.remove(data);
        associateDataList.add(oldIndex, newData);
    }

    /**
     * @param user the user to check for authorization
     * @return whether the user is authorized
     */
    @Override
    public boolean isUnauthorized(final User user) {
        if (!super.isUnauthorized(user)) {
            return false;
        }

        // If the user is not the owner, check if it's an associate
        try {
            getAssociateData(user);
        } catch (OwnershipException e) {
            return true;
        }

        return false;
    }

    /**
     * @param user the user to check the authorization
     * @param card the card to delete
     * @throws OwnershipException if the user is not authorized for this account
     */
    @Override
    public void authorizeCardDeletion(
            final User user,
            final Card card
    ) throws OwnershipException {
        AssociateData associateData = getAssociateData(user);
        if (associateData.role() == Role.EMPLOYEE
                && !card.getCreator().equals(user)) {
            throw new OwnershipException("You are not authorized to make this transaction.");
        }

        super.authorizeCardDeletion(user, card);
    }

    /**
     * @param user the user to check the authorization
     * @throws OwnershipException if the user is not an associate
     * @throws OperationException if the user can't deposit the funds
     * @param amount the amount to spend
     */
    @Override
    public void authorizeDeposit(
            final User user,
            final Amount amount
    ) throws OwnershipException, OperationException {
        if (owner.equals(user)) {
            super.authorizeDeposit(user, amount);
            return;
        }

        AssociateData associateData = getAssociateData(user);

        Amount newDeposited = associateData.deposited().add(amount);

        if (associateData.role() != Role.MANAGER
                && amount.sub(depositLimit).total() > 0.0) {
            throw new OperationException("You are not authorized to make this transaction.");
        }

        super.authorizeDeposit(user, amount);
        updateDeposit(user, newDeposited);
    }

    /**
     * @param user the user to check the authorization
     * @throws OwnershipException if the user is not an associate
     * @throws OperationException if the user can't withdraw the funds
     * @param amount the amount to spend
     */
    @Override
    public void authorizeSpending(
            final User user,
            final Amount amount
    ) throws OwnershipException, OperationException {
        if (owner.equals(user)) {
            super.authorizeSpending(user, amount);
            return;
        }

        AssociateData associateData = getAssociateData(user);

        Amount newSpending = associateData.spent().add(amount);

        if (associateData.role() != Role.MANAGER
                && amount.sub(spendingLimit).total() > 0.0) {
            throw new OperationException("You are not authorized to make this transaction.");
        }

        super.authorizeSpending(user, amount);
        updateSpending(user, newSpending);
    }

    private CommerciantSpending getCommerciantSpending(final String commerciantName) {
        Optional<CommerciantSpending> oSpending =
                commerciantSpendingList
                        .stream()
                        .filter(cs -> cs.getName().equals(commerciantName))
                        .findFirst();

        CommerciantSpending spending = oSpending.orElse(null);

        if (spending == null) {
            spending = new CommerciantSpending(commerciantName, getCurrency());
            commerciantSpendingList.add(spending);
        }

        return spending;
    }

    /**
     * Applies cashback for a given amount
     *
     * @param user the user who made the payment
     * @param commerciant the commerciant that was paid
     * @param amount      the amount that was paid
     */
    @Override
    public void applyCashBack(
            final User user,
            final Commerciant commerciant,
            final Amount amount
    ) {
        super.applyCashBack(user, commerciant, amount);

        // If the user is the owner, don't count them towards the
        // `transaction` report
        if (owner.equals(user)) {
            return;
        }

        CommerciantSpending commerciantSpending = getCommerciantSpending(commerciant.getName());
        commerciantSpending.setReceived(commerciantSpending.getReceived().add(amount));


        AssociateData associateData = getAssociateData(user);
        switch (associateData.role()) {
            case EMPLOYEE -> commerciantSpending.getEmployees().add(
                    associateData.associate().getUsername()
            );
            case MANAGER -> commerciantSpending.getManagers().add(
                    associateData.associate().getUsername()
            );
            default -> {

            }
        }

    }
}
