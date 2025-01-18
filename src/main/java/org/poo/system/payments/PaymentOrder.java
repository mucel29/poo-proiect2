package org.poo.system.payments;

import org.poo.system.Transaction;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

public record PaymentOrder(
        PendingPayment payment,
        Account account,
        Amount amount,
        Transaction transaction
) {
}
