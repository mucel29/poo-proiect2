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
import org.poo.system.user.User;

public class DeleteAccountCommand extends Command.Base {

    private final String account;
    private final String email;

    public DeleteAccountCommand(
            final String account,
            final String email
    ) {
        super(Type.DELETE_ACCOUNT);
        this.account = account;
        this.email = email;
    }


    /**
     * {@inheritDoc}
     * @throws UserNotFoundException if no user with the given email was found
     * @throws OwnershipException if the given account is not owned by the given user
     * @throws OperationException if the account still has funds
     */
    @Override
    public void execute() throws UserNotFoundException, OwnershipException, OperationException {
        // Retrieve the user from the storage provider
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        // Check if the given user matches the account owner
        // NOTE: this could've been checked more easily
        // by retrieving the target account
        // and checking the owner's email with the provided email
        // but the storage provider also check if the user / account is registered
        if (!BankingSystem.getStorageProvider().getUserByIban(account).equals(targetUser)) {
            throw new OwnershipException("Account " + account + " does not belong to " + email);
        }

        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);

        // If the user still has funds, generate errors
        if (targetAccount.getFunds() > 0) {
            throw new OperationException(
                    "Account couldn't be deleted - see org.poo.transactions for details",
                    "Account " + account + " still has funds!",
                    new TransactionHandler(
                            targetAccount,
                            timestamp,
                            "Account couldn't be deleted - there are funds remaining"
                    ),
                    new CommandErrorHandler(this)
            );
        }

        // Remove the account and output success
        BankingSystem.getStorageProvider().removeAccount(targetAccount);
        super.output(obj -> {
            obj.put(
                    "success",
                    "Account deleted"
            );
            obj.put("timestamp", timestamp);
        });
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");

        return new DeleteAccountCommand(account, email);
    }

}
