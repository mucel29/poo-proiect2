package org.poo.system.payments;

import lombok.Getter;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;
import org.poo.utils.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class SplitPayment implements PendingPayment {

    private final PendingPayment.Type type;
    private final Amount totalAmount;
    private final int timestamp;
    private final List<Pair<Account, Amount>> involvedAccounts = new ArrayList<>();
    private final Set<PaymentObserver> observers = new HashSet<>();
    private final List<PaymentObserver> accepted = new ArrayList<>();

    public SplitPayment(
            final PendingPayment.Type type,
            final Amount totalAmount,
            final int timestamp
    ) {
        this.type = type;
        this.totalAmount = totalAmount;
        this.timestamp = timestamp;
    }

    private Transaction.SplitPayment generateTransaction() {
        Transaction.SplitPayment splitTransaction = new Transaction.SplitPayment(
                "Split payment of "
                        + String.format("%.2f", totalAmount.total())
                        + " "
                        + totalAmount.currency(),
                timestamp

        )
                .setSplitPaymentType(type.toString())
                .setInvolvedAccounts(
                involvedAccounts
                        .stream()
                        .map(entry -> entry.first().getAccountIBAN())
                        .toList()
                ).setCurrency(totalAmount.currency());

        // Set the `amount` or `amountForUsers` list
        if (type == Type.EQUAL) {
            splitTransaction
                    .setAmount(totalAmount.total() / involvedAccounts.size());
        } else {
            splitTransaction
                    .setAmountForUsers(
                            involvedAccounts.stream().map(
                                    entry -> entry.second().total()
                            ).toList()
                    );
        }

        return splitTransaction;
    }

    private void complete() {
        Transaction.SplitPayment splitTransaction = generateTransaction();

        // Check if everyone can pay
        for (var involvedEntry : involvedAccounts) {
            if (!involvedEntry.first().canPay(involvedEntry.second(), true)) {

                splitTransaction.setError(
                        "Account "
                                + involvedEntry.first().getAccountIBAN()
                                + " has insufficient funds for a split payment."
                );
                break;
            }
        }

        // Notify all observers of the payment result
        // Whether the payment should be made or not
        notifyAll(splitTransaction);
    }

    private void notifyAll(final Transaction.SplitPayment splitTransaction) {
        for (PaymentObserver observer : observers) {
            for (var involvedEntry : involvedAccounts) {

                // Check if the user owns the account
                // If not, continue
                if (!observer.owns(involvedEntry.first())) {
                    continue;
                }

                Amount amount = involvedEntry.second();

                // If the payment was rejected (or someone didn't have enough funds)
                // Set the amount to 0
                if (splitTransaction.getError() != null) {
                    amount = Amount.zero(involvedEntry.first().getCurrency());
                }

                // Create the order
                PaymentOrder order = new PaymentOrder(
                        this,
                        involvedEntry.first(),
                        amount,
                        splitTransaction
                );

                // Notify the observer
                // (to deduct the amount or to just remove the payment)
                observer.notify(order);
            }
        }
    }

    /**
     * Accepts the payment
     *
     * @param observer the one observing the payment
     */
    @Override
    public void accept(final PaymentObserver observer) {
        BankingSystem.log(
                observer
                        + " accepted split from timestamp "
                        + timestamp
        );
        // Mark the entry as accepted
        accepted.add(observer);

        // Check if everyone accepted the payment
        if (observers.size() == accepted.size()) {
            complete();
        }
    }

    /**
     * Rejects the payment
     *
     * @param observer the one observing the payment
     */
    @Override
    public void reject(final PaymentObserver observer) {
        BankingSystem.log(
                observer + " rejected split from timestamp " + timestamp
        );
        // Add the rejected error message
        Transaction.SplitPayment splitTransaction =
                generateTransaction().setError("One user rejected the payment.");

        // Notify everyone of the failure
        notifyAll(splitTransaction);
    }

    /**
     * Checks whether the payment was dealt with by the given observer
     *
     * @param observer the one observing the payment
     * @return if the payment was dealt with by the observer
     */
    @Override
    public boolean wasDealt(final PaymentObserver observer) {
        return accepted.contains(observer);
    }
}
