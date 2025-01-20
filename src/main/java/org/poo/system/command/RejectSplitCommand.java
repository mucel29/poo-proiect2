package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.UserNotFoundException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.payments.PendingPayment;
import org.poo.system.user.User;

public class RejectSplitCommand extends Command.Base {


    private final String email;
    private final PendingPayment.Type paymentType;

    public RejectSplitCommand(
            final String email,
            final PendingPayment.Type paymentType
    ) {
        super(Type.REJECT_SPLIT);
        this.email = email;
        this.paymentType = paymentType;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if the given email doesn't match a user
     * @throws OperationException if there are no unaddressed
     * pending payments of the requested type
     */
    @Override
    public void execute() throws UserNotFoundException, OperationException {
        // Retrieve the user
        User targetUser;
        try {
            targetUser = BankingSystem.getStorageProvider().getUserByEmail(email);
        } catch (UserNotFoundException e) {
            throw new UserNotFoundException(
                    "User not found",
                    null,
                    new CommandDescriptionHandler(this)
            );
        }

        // Rejects the payment, notify the subject
        targetUser.getFirstPending(paymentType).reject(targetUser);
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
        PendingPayment.Type paymentType = PendingPayment.Type.fromString(
                IOUtils.readStringChecked(node, "splitPaymentType")
        );

        return new RejectSplitCommand(email, paymentType);
    }

}
