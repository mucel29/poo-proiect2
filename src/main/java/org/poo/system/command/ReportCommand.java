package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;

public class ReportCommand extends Command.Base {

    private final String account;
    private final int startTimestamp;
    private final int endTimestamp;

    public ReportCommand(
            final String account,
            final int startTimestamp,
            final int endTimestamp
    ) {
        super(Type.REPORT);
        this.account = account;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        Account targetAccount;
        try {
             targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);
        } catch (OwnershipException e) {
            super.output(root -> {
                root.put("description", "Account not found");
                root.put("timestamp", timestamp);
            });
            return;
        }
        super.output((root) -> {
            root.put("balance", targetAccount.getFunds());
            root.put("IBAN", account);
            root.put("currency", targetAccount.getCurrency());
            ArrayNode arr = root.putArray("transactions");
            for (Transaction transaction : targetAccount.getTransactions()) {
                if (
                        transaction.getTimestamp() >= startTimestamp
                                && transaction.getTimestamp() <= endTimestamp
                ) {
                    arr.add(transaction.toNode());
                }
            }
        });

    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        int startTimestamp = IOUtils.readIntChecked(node, "startTimestamp");
        int endTimestamp = IOUtils.readIntChecked(node, "endTimestamp");

        return new ReportCommand(account, startTimestamp, endTimestamp);
    }

}
