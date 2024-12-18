package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.UserNotFoundException;
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

    private void export(final boolean success) {
        super.output(obj -> {
            if (success) {
                obj.put(
                        "success",
                        "Account deleted"
                );
            } else {
                obj.put(
                        "error",
                        "Account couldn't be deleted - see org.poo.transactions for details"
                );
            }
            obj.put("timestamp", timestamp);
        });
    }

    /**
     * {@inheritDoc}
     * @throws UserNotFoundException if no user with the given email was found
     * @throws OwnershipException if the given account is not owned by the given user
     * @throws OperationException if the account still has funds
     */
    @Override
    public void execute() throws UserNotFoundException, OwnershipException, OperationException {
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        if (!BankingSystem.getStorageProvider().getUserByIban(account).equals(targetUser)) {
            throw new OwnershipException("Account " + account + " does not belong to " + email);
        }

        Account targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);

        if (targetAccount.getFunds() > 0) {
            export(false);
            targetAccount.getTransactions().add(new Transaction.Base(
                    "Account couldn't be deleted - there are funds remaining",
                    timestamp
            ));
            throw new OperationException("Account " + account + " still has funds!");
        }

//        targetUser.getAccounts().remove(targetAccount);
        BankingSystem.getStorageProvider().removeAccount(targetAccount);
        export(true);
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws BankingInputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws BankingInputException {
        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");

        return new DeleteAccountCommand(account, email);
    }

}
