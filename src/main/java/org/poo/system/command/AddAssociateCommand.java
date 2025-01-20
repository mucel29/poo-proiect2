package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exceptions.handlers.CommandErrorHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.user.Account;
import org.poo.system.user.BusinessAccount;
import org.poo.system.user.User;

public class AddAssociateCommand extends Command.Base {

    private final BusinessAccount.Role role;
    private final String email;
    private final String account;

    public AddAssociateCommand(
            final BusinessAccount.Role role,
            final String email,
            final String account
    ) {
        super(Type.ADD_ASSOCIATE);
        this.role = role;
        this.email = email;
        this.account = account;
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnershipException if no user owns the given account
     * @throws UserNotFoundException if the user could not be found
     * @throws OperationException if the user is already an associate
     */
    @Override
    public void execute() throws OwnershipException, UserNotFoundException, OperationException {
        // Retrieve the account and user
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

        // Check if the account is a bussiness one
        if (targetAccount.getAccountType() != Account.Type.BUSINESS) {
            throw new OwnershipException(
                    "Account is not of type business",
                    null,
                    new CommandErrorHandler(this)
            );
        }

        BusinessAccount bAccount = (BusinessAccount) targetAccount;

        // Add the associate
        try {
            bAccount.addAssociate(targetUser, role);
        } catch (OperationException e) {
            throw new OperationException(
                    "The user is already an associate of the account.",
                    e.getMessage(),
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        String email = IOUtils.readStringChecked(node, "email");
        BusinessAccount.Role role = BusinessAccount.Role.fromString(
                IOUtils.readStringChecked(node, "role")
        );

        return new AddAssociateCommand(role, email, account);
    }

}
