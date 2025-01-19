package org.poo.system.commerce.cashback;

import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

public final class TransactionStrategy extends CommerciantStrategy.Base {

    public TransactionStrategy(final Commerciant.Type commerciantType) {
        super(commerciantType);
    }

    /**
     * Applies a cashback based on the number of transactions made to a category
     * {@inheritDoc}
     */
    @Override
    public Amount apply(
            final Account account,
            final Amount amount
    ) {
        CommerciantData data = super.getCommerciantData(account);
        data.setTransactionCount(data.getTransactionCount() + 1);

        if (data.getTransactionCount() == commerciantType.getTransactionThreshold()) {
            BankingSystem.log(
                    "Applied transaction cashback to "
                    + account.getAccountIBAN()
            );
            return new Amount(
                            amount.total()
                            * commerciantType.getTransactionCashback(),
                    amount.currency()
            );
        }

        return new Amount(0, amount.currency());
    }
}
