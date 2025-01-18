package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.payments.PendingPayment;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exchange.Amount;
import org.poo.system.payments.SplitPayment;
import org.poo.system.user.Account;
import org.poo.utils.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

public class SplitPayCommand extends Command.Base {

    private final PendingPayment.Type type;
    private final String currency;
    private final Amount totalAmount;
    private final List<Pair<String, Amount>> userAmounts = new ArrayList<>();

    public SplitPayCommand(
            final PendingPayment.Type type,
            final String currency,
            final List<Pair<String, Amount>> userAmounts
    ) {
        super(Type.SPLIT_PAYMENT);
        this.type = type;
        this.currency = currency;
        this.userAmounts.addAll(userAmounts);
        this.totalAmount = userAmounts.stream().map(Pair::second).reduce(
                new Amount(0, currency), Amount::add
        );
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnershipException if any of the accounts isn't owned by a user
     */
    @Override
    public void execute() throws OwnershipException {
        BankingSystem.log("total amount to be paid: " + totalAmount);
        SplitPayment payment = new SplitPayment(type, totalAmount, timestamp);
        for (var userAmount : userAmounts) {
            Account involvedAccount = BankingSystem
                    .getStorageProvider()
                    .getAccountByIban(userAmount.first());

            payment.getInvolvedAccounts().add(new Pair<>(
                    involvedAccount,
                    userAmount.second()
            ));

            involvedAccount.getOwner().getPendingPayments().add(payment);
        }
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     *
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        JsonNode accountsNode = node.get("accounts");
        if (accountsNode == null || !accountsNode.isArray()) {
            throw new InputException("accounts must be an array");
        }

        List<String> accounts = new ArrayList<>();
        for (JsonNode account : accountsNode) {
            accounts.add(account.asText());
        }

        String currency = IOUtils.readStringChecked(node, "currency");
        List<Amount> amounts = new ArrayList<>();

        PendingPayment.Type type = PendingPayment.Type.fromString(
                IOUtils.readStringChecked(node, "splitPaymentType")
        );

        switch (type) {
            case PendingPayment.Type.EQUAL:
                double amount = IOUtils.readDoubleChecked(node, "amount");
                amounts.addAll(Collections.nCopies(
                        accounts.size(),
                        new Amount(amount / accounts.size(), currency)
                ));
                break;
            case PendingPayment.Type.CUSTOM:
                JsonNode amountsNode = node.get("amountForUsers");
                if (amountsNode == null || !amountsNode.isArray()) {
                    throw new InputException("amountForUsers must be an array");
                }

                for (JsonNode amountNode : amountsNode) {
                    amounts.add(new Amount(amountNode.asDouble(), currency));
                }
                break;
            default:
                break;
        }

        if (accounts.size() != amounts.size()) {
            throw new InputException(
                    "Number of accounts doesn't match the number of amounts"
            );
        }

        List<Pair<String, Amount>> userAmounts =
                IntStream.range(0, accounts.size())
                        .boxed()
                        .map(index -> new Pair<>(
                                accounts.get(index),
                                amounts.get(index)
                        )).toList();

        return new SplitPayCommand(type, currency, userAmounts);
    }

}
