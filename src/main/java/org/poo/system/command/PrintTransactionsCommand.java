package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.User;

public class PrintTransactionsCommand extends Command.Base {

    private final String email;

    public PrintTransactionsCommand(final String email) {
        super(Command.Type.PRINT_TRANSACTIONS);
        this.email = email;
    }

    /**
     * @throws UserNotFoundException if the given user does not exist
     */
    @Override
    public void execute() throws UserNotFoundException {
        User targetUser = BankingSystem.getUserByEmail(email);
        super.outputArray(
                arr -> targetUser
                        .getTransactions()
                        .forEach(
                                transaction -> {
                                    try {
                                        arr.add(transaction.toNode());
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                        )
        );
    }

    /**
     * Deserializes the given node into a `Command.Base` instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws BankingInputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws BankingInputException {
        String email = IOUtils.readStringChecked(node, "email");

        return new PrintTransactionsCommand(email);
    }

}
