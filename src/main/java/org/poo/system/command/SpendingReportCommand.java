package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.user.Account;

import java.util.HashMap;
import java.util.Map;

public class SpendingReportCommand extends Command.Base {

    private final String account;
    private final int startTimestamp;
    private final int endTimestamp;

    public SpendingReportCommand(
            final String account,
            final int startTimestamp,
            final int endTimestamp
    ) {
        super(Type.SPENDINGS_REPORT);
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
        if (targetAccount.getAccountType() == Account.Type.SAVINGS) {
            super.output(root -> {
                root.put(
                        "error",
                        "This kind of report is not supported for a saving account"
                );
            });
            return;
        }
        super.output((root) -> {
            root.put("balance", targetAccount.getFunds());
            root.put("IBAN", account);
            root.put("currency", targetAccount.getCurrency());
            ArrayNode arr = root.putArray("transactions");
            Map<String, Double> commerciants = new HashMap<>();

            for (Transaction transaction : targetAccount.getTransactions()) {
                String commerciant = transaction.getCommerciant();
                double amount = transaction.getAmount();
                if (commerciant == null || commerciant.isEmpty() || amount <= 0) {
                    continue;
                }
                if (transaction.getTimestamp() >= startTimestamp
                        && transaction.getTimestamp() <= endTimestamp
                ) {
                    arr.add(transaction.toNode());
                    if (!commerciants.containsKey(commerciant)) {
                        commerciants.put(commerciant, 0.0);
                    }
                    commerciants.put(
                            commerciant,
                            commerciants.get(commerciant)
                                    + amount
                    );
                }
            }

            ArrayNode comArr = root.putArray("commerciants");
            commerciants
                    .keySet()
                    .stream()
                    .sorted(String::compareTo)
                    .forEach(
                            commerciant -> {
                                ObjectNode comObj = comArr.addObject();
                                comObj.put("commerciant", commerciant);
                                comObj.put("total", commerciants.get(commerciant));
                            }
                    );

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

        return new SpendingReportCommand(account, startTimestamp, endTimestamp);
    }

}
