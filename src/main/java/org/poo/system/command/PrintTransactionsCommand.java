package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.User;

public class PrintTransactionsCommand extends Command.Base {

    private final String email;

    public PrintTransactionsCommand(final String email) {
        super(Command.Type.PRINT_TRANSACTIONS);
        this.email = email;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if the given user does not exist
     */
    @Override
    public void execute() throws UserNotFoundException {
        // Retrieve the user from the storage provider
        User targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);

        // Generate command output
        super.outputArray(
                arr -> targetUser.getTransactions().forEach(transaction -> {
                    try {
                        arr.add(transaction.toNode());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
        );
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     *
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String email = IOUtils.readStringChecked(node, "email");

        return new PrintTransactionsCommand(email);
    }

}
