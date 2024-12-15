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

    private final String IBAN;
    private final String email;

    public DeleteAccountCommand(String IBAN, String email) {
        super(Type.DELETE_ACCOUNT);
        this.IBAN = IBAN;
        this.email = email;
    }

    private void export(final boolean success) {
        super.output(obj -> {
            if (success) {
                obj.put("success", "Account deleted");
            } else {
                obj.put("error", "Account couldn't be deleted - see org.poo.transactions for details");
            }
            obj.put("timestamp", timestamp);
        });
    }

    @Override
    public void execute() throws UserNotFoundException, OwnershipException, OperationException {
        User targetUser = BankingSystem.getUserByEmail(email);
        if (!BankingSystem.getUserByIBAN(IBAN).equals(targetUser)) {
            throw new OwnershipException("Account " + IBAN + " does not belong to " + email);
        }

        Account targetAccount = BankingSystem.getAccount(IBAN);

        if (targetAccount.getFunds() > 0) {
            export(false);
            targetAccount.getTransactions().add(new Transaction.Base(
                    "Account couldn't be deleted - there are funds remaining",
                    timestamp
            ));
            throw new OperationException("Account " + IBAN + " still has funds!");
        }

        targetUser.getAccounts().remove(targetAccount);
        export(true);
    }

    public static DeleteAccountCommand fromNode(final JsonNode node) throws BankingInputException {
        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");

        return new DeleteAccountCommand(account, email);
    }

}
