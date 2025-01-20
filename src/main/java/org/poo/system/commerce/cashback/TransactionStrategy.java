package org.poo.system.commerce.cashback;

import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

import java.util.Map;

public final class TransactionStrategy extends CommerciantStrategy.Base {

    public TransactionStrategy(final Commerciant commerciant) {
        super(commerciant);
    }

    private static final Map<Integer, Commerciant.Type> TRANSACTION_THRESHOLDS =
            Map.of(
                    2, Commerciant.Type.FOOD,
                    5, Commerciant.Type.CLOTHES,
                    10, Commerciant.Type.TECH
            );

    /**
     * {@inheritDoc}
     *
     * Applies a cashback based on the number of transactions made to a commerciant
     */
    @Override
    public Amount apply(
            final Account account,
            final Amount amount
    ) {
        // Add the transaction the commerciant
        commerciant.addTransaction(account);

        BankingSystem.log(
                account.getAccountIBAN()
                + " ["
                + commerciant.getTransactionCount(account)
                + " / "
                + commerciant.getType().getTransactionThreshold()
                + "]"
        );

        // Check if any transaction threshold was reached
        Commerciant.Type coupon = TRANSACTION_THRESHOLDS
                .get(commerciant.getTransactionCount(account));
        if (coupon != null) {

            // Activate the coupon if it wasn't redeemed
            if (account.getCoupons().containsKey(coupon)) {
                account.getCoupons().put(coupon, true);
            }

            return Amount.zero(amount.currency());
        }

        return super.applyCoupon(account, amount);
    }
}
