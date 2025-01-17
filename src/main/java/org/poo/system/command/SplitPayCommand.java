package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SplitPayCommand extends Command.Base {

    private final List<String> accounts = new ArrayList<>();
    private final Amount totalAmount;

    public SplitPayCommand(
            final List<String> accounts,
            final Amount totalAmount
    ) {
        super(Type.SPLIT_PAYMENT);
        this.accounts.addAll(accounts);
        this.totalAmount = totalAmount;
    }

    /**
     * {@inheritDoc}
     * @throws OwnershipException if any of the accounts isn't owned by a user
     */
    @Override
    public void execute() throws OwnershipException {
        // Calculate the total to be paid be each account
        Amount amount = new Amount(totalAmount.total() / accounts.size(), totalAmount.currency());

        // Retrieve all the accounts from the storage provider
        List<Account> targetAccounts = accounts.stream().map(
                BankingSystem.getStorageProvider()::getAccountByIban
        ).toList();

        // Map to store the deducted amounts in the account's currency
        Map<Account, Amount> deducted = new ConcurrentHashMap<>();

        // Create a split transaction
        Transaction.SplitPayment transaction =
                new Transaction.SplitPayment(

                        "Split payment of "
                                + String.format("%.2f", totalAmount.total())
                                + " "
                                + totalAmount.currency(),
                        timestamp
                )
                        .setAmount(amount.total())
                        .setCurrency(amount.currency())
                        .setInvolvedAccounts(accounts);

        // Check if all accounts can pay and compute their deducted total
        targetAccounts.forEach((account) -> {

            Amount localAmount = amount.to(account.getCurrency());

            // TODO: add fees???

            // If an account doesn't have enough funds,
            // set the transaction as an erroneous one
            if (account.getFunds().total() < localAmount.total()) {
                transaction.setError(
                        "Account "
                                + account.getAccountIBAN()
                                + " has insufficient funds for a split payment."
                );
                return;
            }

            // Store total to be deducted in the account's currency
            deducted.put(account, localAmount);

        });

        // Add transaction to user and possibly deduct total
        targetAccounts.parallelStream().forEach((account) -> {
           account.getTransactions().add(transaction);

           // Everyone can pay
           if (transaction.getError() == null) {
               account.setFunds(account.getFunds().sub(deducted.get(account)));
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
        JsonNode accountsNode = node.get("accounts");
        if (accountsNode == null || !accountsNode.isArray()) {
            throw new InputException("accounts must be an array");
        }
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String currency = IOUtils.readStringChecked(node, "currency");

        List<String> accounts = new ArrayList<>();
        for (JsonNode account : accountsNode) {
            accounts.add(account.asText());
        }
        return new SplitPayCommand(accounts, new Amount(amount, currency));
    }

}
