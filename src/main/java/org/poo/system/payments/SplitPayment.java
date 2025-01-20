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

        if (type == Type.EQUAL) {
            splitTransaction
                    .setAmount(totalAmount.total() / involvedAccounts.size());
        } else {
            // Might produce out of order values ???
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
        // Maybe check if all observers own at least 1 account
        Transaction.SplitPayment splitTransaction = generateTransaction();

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

        notifyAll(splitTransaction);
    }

    private void notifyAll(final Transaction.SplitPayment splitTransaction) {
        for (PaymentObserver observer : observers) {
            for (var involvedEntry : involvedAccounts) {

                if (!observer.owns(involvedEntry.first())) {
                    continue;
                }

                Amount amount = involvedEntry.second();
                if (splitTransaction.getError() != null) {
                    amount = new Amount(0, involvedEntry.first().getCurrency());
                }

                PaymentOrder order = new PaymentOrder(
                        this,
                        involvedEntry.first(),
                        amount,
                        splitTransaction
                );

                observer.notify(order);
            }
        }
    }

    private void printExpected() {
        StringBuilder expected = new StringBuilder();
        for (PaymentObserver observer : observers) {
            expected
                    .append(observer.toString())
                    .append(" [")
                    .append(accepted.contains(observer))
                    .append("]\n");
        }

        BankingSystem.log(
                "====== ["
                        + timestamp
                        + "] =====\n"
                        + expected
        );
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
        // Mark the entry as accepted (maybe check if it owns any account first?)
        accepted.add(observer);

//        BankingSystem.log(
//                accepted.size()
//                        + " / "
//                        + observers.size()
//                        +
//                        " for ["
//                        + timestamp
//                        + "]\n"
//        );
//        printExpected();
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
        Transaction.SplitPayment splitTransaction = generateTransaction()
                .setError("One user rejected the payment.");

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
