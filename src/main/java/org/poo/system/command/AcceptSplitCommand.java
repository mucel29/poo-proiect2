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
import org.poo.utils.Pair;

import java.util.Optional;

public class AcceptSplitCommand extends Command.Base {

    private final String email;
    private final PendingPayment.Type paymentType;

    public AcceptSplitCommand(
            final String email,
            final PendingPayment.Type paymentType
    ) {
        super(Type.ACCEPT_SPLIT);
        this.email = email;
        this.paymentType = paymentType;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UserNotFoundException if the given email doesn't match a user
     */
    @Override
    public void execute() throws UserNotFoundException {
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

        Optional<PendingPayment> payment = targetUser.getPendingPayments()
                .stream().map(Pair::first).filter(
                pendingPayment -> pendingPayment.getType() == paymentType
                        && !pendingPayment.wasDealt(targetUser)
        ).findFirst();

        if (payment.isEmpty()) {
            throw new OperationException("No pending " + paymentType + " payment found");
        }

        payment.get().accept(targetUser);
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

        return new AcceptSplitCommand(email, paymentType);
    }

}
