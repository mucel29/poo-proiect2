package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.user.User;

public class PrintTransactionsCommand extends Command.Base {

    private final String email;

    public PrintTransactionsCommand(String email) {
        super(Command.Type.PRINT_TRANSACTIONS);
        this.email = email;
    }

    @Override
    public void execute() throws UserNotFoundException {
        User targetUser = BankingSystem.getUserByEmail(email);
        super.outputArray(
                arr -> targetUser
                        .getTransactions()
                        .stream()
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

    public static PrintTransactionsCommand fromNode(final JsonNode node) throws BankingInputException {
        String email = node.get("email").asText();
        if (email.isEmpty()) {
            throw new BankingInputException("Missing email for PrintTransactions\n" + node.toPrettyString());
        }

        return new PrintTransactionsCommand(email);
    }

}
