package org.poo.system.commerce.cashback;

import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.user.Account;

public final class TransactionStrategy extends CommerciantStrategy.Base {

    public TransactionStrategy(final Commerciant.Type commerciantType) {
        super(commerciantType);
    }

    /**
     * Applies the strategy on the given account
     *
     * @param account the account on which to apply the strategy on
     * @param amount  the amount to be paid in RON
     */
    @Override
    public void apply(
            final Account account,
            final double amount
    ) {
        CommerciantData data = super.getCommerciantData(account);
        data.setSpending(data.getSpending() + amount);
        data.setTransactionCount(data.getTransactionCount() + 1);

        if (data.getTransactionCount() == commerciantType.getTransactionThreshold()) {
            double convertedAmount =
                    amount
                    * BankingSystem
                        .getExchangeProvider()
                        .getRate(
                                "RON", account.getCurrency()
                        );
            account.setFunds(
                    account.getFunds()
                            + convertedAmount * commerciantType.getTransactionThreshold()
            );
        }

    }
}
