package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.CommandErrorHandler;
import org.poo.system.user.Account;
import org.poo.system.user.BusinessAccount;
import org.poo.system.user.User;

import java.util.Arrays;
import java.util.NoSuchElementException;

public final class ChangeLimitCommand extends Command.Base {

    @Getter
    public enum Type {
        SPENDING(Command.Type.CHANGE_SPENDING_LIMIT),
        DEPOSIT(Command.Type.CHANGE_DEPOSIT_LIMIT);

        private final Command.Type type;

        Type(final Command.Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        /**
         * Converts a String to an {@code ChangeLimitCommand.Type}
         *
         * @param label the string to convert
         * @return the corresponding {@code ChangeLimitCommand.Type}
         * @throws InputException if the label can't
         * be converted to an {@code ChangeLimitCommand.Type}
         */
        public static ChangeLimitCommand.Type fromString(
                final String label
        ) throws InputException {
            try {
                return Arrays
                        .stream(ChangeLimitCommand.Type.values())
                        .filter(type -> type.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown limit type: " + label);
            }
        }

    }

    private final Type type;
    private final String email;
    private final String account;
    private final double amount;

    private ChangeLimitCommand(
            final Type type,
            final String email,
            final String account,
            final double amount
    ) {
        super(type.getType());
        this.type = type;
        this.email = email;
        this.account = account;
        this.amount = amount;
    }

    /**
     * Changes the limit for the business account
     * @throws OwnershipException
     * @throws OperationException
     */
    public void execute() throws OwnershipException, OperationException {

        Account targetAccount;
        User targetUser;
        try {
            targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);
            targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        } catch (OwnershipException e) {
            throw new OwnershipException(
                    "Account not found",
                    e.getMessage(),
                    new CommandErrorHandler(this)
            );
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException(
                    "User not found",
                    e.getMessage(),
                    new CommandErrorHandler(this)
            );
        }

        if (targetAccount.getAccountType() != Account.Type.BUSINESS) {
            throw new OwnershipException(
                    "This is not a business account",
                    null,
                    new CommandDescriptionHandler(this)
            );
        }

        BusinessAccount bAccount = (BusinessAccount) targetAccount;

        if (!bAccount.getOwner().equals(targetUser)) {
            throw new OwnershipException(
                    "You must be owner in order to change "
                            + type
                            + " limit.",
                    null,
                    new CommandDescriptionHandler(this)
            );
        }

        switch (type) {
            case SPENDING:
                bAccount.setSpendingLimit(bAccount.getSpendingLimit().set(amount));
                break;
            case DEPOSIT:
                bAccount.setDepositLimit(bAccount.getDepositLimit().set(amount));
                break;
            default:
                break;
        }
        // Add transaction???


    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(
            final JsonNode node,
            final ChangeLimitCommand.Type type
    ) throws InputException {

        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");
        double amount = IOUtils.readDoubleChecked(node, "amount");

        return new ChangeLimitCommand(type, email, account, amount);
    }

}
