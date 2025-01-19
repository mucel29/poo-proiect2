package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.CommandErrorHandler;
import org.poo.system.user.Account;
import org.poo.system.user.AssociateData;
import org.poo.system.user.BusinessAccount;
import org.poo.system.user.User;

import java.util.Arrays;
import java.util.NoSuchElementException;

public final class BusinessReportCommand extends Command.Base {

    enum Type {
        TRANSACTION,
        COMMERCIANT;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        /**
         * Converts a String to an {@code BusinessReportCommand.Type}
         *
         * @param label the string to convert
         * @return the corresponding {@code BusinessReportCommand.Type}
         * @throws InputException if the label can't be converted to an
         * {@code BusinessReportCommand.Type}
         */
        public static BusinessReportCommand.Type fromString(
                final String label
        ) throws InputException {
            try {
                return Arrays
                        .stream(BusinessReportCommand.Type.values())
                        .filter(type -> type.toString().equals(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown business report type: " + label);
            }
        }

    }

    private final Type type;
    private final String account;
    private final int startTimestamp;
    private final int endTimestamp;

    private BusinessReportCommand(
            final Type type,
            final String account,
            final int startTimestamp,
            final int endTimestamp
    ) {
        super(Command.Type.BUSINESS_REPORT);
        this.type = type;
        this.account = account;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    private int compareData(
            final AssociateData data1,
            final AssociateData data2
    ) {
        User user1 = data1.associate();
        User user2 = data2.associate();

        int depositCompare = Double.compare(
                data2.deposited().total(),
                data1.deposited().total()
        );

        if (depositCompare != 0) {
            return depositCompare;
        }

        String username1 = user1.getLastName() + " " + user1.getFirstName();
        String username2 = user2.getLastName() + " " + user2.getFirstName();

        return username1.compareTo(username2);
    }

    @Override
    public void execute() throws OwnershipException, OperationException {
        Account targetAccount;
        try {
            targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);
        } catch (OwnershipException e) {
            throw new OperationException(
                    "Account not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        }

        if (targetAccount.getAccountType() != Account.Type.BUSINESS) {
            throw new OwnershipException(
                    "Account is not of type business",
                    null,
                    new CommandErrorHandler(this)
            );
        }

        BusinessAccount bAccount = (BusinessAccount) targetAccount;

        // Print the account's transaction record
        super.output((root) -> {
            root.put("balance", targetAccount.getFunds().total());
            root.put("IBAN", account);
            root.put("currency", targetAccount.getCurrency());
            root.put("spending limit", bAccount.getSpendingLimit().total());
            root.put("deposit limit", bAccount.getDepositLimit().total());
            root.put("statistics type", type.toString());

            ArrayNode managerArray = root.putArray("managers");
            ArrayNode employeeArray = root.putArray("employees");

            // Compute total
            double totalSpent = 0.0;
            double totalDeposited = 0.0;

            bAccount.getAssociateDataList().sort(this::compareData);

            for (AssociateData aData : bAccount.getAssociateDataList()) {
                switch (aData.role()) {
                    case MANAGER -> managerArray.add(aData.toNode());
                    case EMPLOYEE -> employeeArray.add(aData.toNode());
                    default -> {
                    }
                }
                totalSpent += aData.spent().total();
                totalDeposited += aData.deposited().total();

            }

            root.put("total spent", totalSpent);
            root.put("total deposited", totalDeposited);

        });

    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     *
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        int startTimestamp = IOUtils.readIntChecked(node, "startTimestamp");
        int endTimestamp = IOUtils.readIntChecked(node, "endTimestamp");
        Type type = Type.fromString(IOUtils.readStringChecked(node, "type"));

        return new BusinessReportCommand(type, account, startTimestamp, endTimestamp);
    }

}
