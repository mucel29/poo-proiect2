package org.poo.system.payments;

import org.poo.system.user.Account;

public interface PaymentObserver {

    /**
     * Registers the pending payment to the observer
     *
     * @param payment the payment to register
     */
    void register(PendingPayment payment);

    /**
     * Notifies the observer of a payment to be made
     *
     * @param order the payment order to execute
     */
    void notify(PaymentOrder order);

    /**
     * @param account the account to check
     * @return whether the observer owns the given account
     */
    boolean owns(Account account);

}
