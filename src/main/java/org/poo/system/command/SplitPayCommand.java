package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;
import org.poo.system.exchange.Exchange;
import org.poo.system.user.Account;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SplitPayCommand extends Command.Base {

    private final List<String> accounts = new ArrayList<>();
    private final String currency;
    private final double totalAmount;

    public SplitPayCommand(List<String> accounts, String currency, double totalAmount) {
        super(Type.SPLIT_PAYMENT);
        this.accounts.addAll(accounts);
        this.currency = currency;
        this.totalAmount = totalAmount;
    }

    @Override
    public void execute() {
        // TODO: feedback, multiple accounts could have insufficient funds!
        double amount = totalAmount / accounts.size();
        List<Account> targetAccounts = accounts.stream().map(BankingSystem::getAccount).toList();
        Map<Account, Double> deducted = new ConcurrentHashMap<>();

        Transaction.SplitPayment transaction =
                new Transaction.SplitPayment(
                        "Split payment of " + String.format("%.2f", totalAmount) + " " + currency, timestamp
                )
                        .setAmount(amount)
                        .setCurrency(currency)
                        .setInvolvedAccounts(accounts);

        // Check if all accounts can pay and compute their deducted amount
        targetAccounts.forEach((account) -> {
            // Stop evaluating if an error already occurred
//            if (transaction.getError() != null) {
//                return;
//            }

            double localAmount = amount * Exchange.getRate(currency, account.getCurrency());
            if (account.getFunds() < localAmount) {
                transaction.setError("Account " + account.getIBAN() + " has insufficient funds for a split payment.");
                return;
            }

            deducted.put(account, localAmount);

        });

        // Add transaction to user and possibly deduct amount
        targetAccounts.parallelStream().forEach((account) -> {
           account.getTransactions().add(transaction);
           if (transaction.getError() == null) {
               account.setFunds(account.getFunds() - deducted.get(account));
           }
        });
    }

    public static SplitPayCommand fromNode(final JsonNode node) throws BankingInputException {
        JsonNode accountsNode = node.get("accounts");
        if (accountsNode == null || !accountsNode.isArray()) {
            throw new BankingInputException("accounts must be an array");
        }
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String currency = IOUtils.readStringChecked(node, "currency");

        List<String> accounts = new ArrayList<>();
        for (JsonNode account : accountsNode) {
            accounts.add(account.asText());
        }
        return new SplitPayCommand(accounts, currency, amount);
    }

}
