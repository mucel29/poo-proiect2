package org.poo.system.user.plan;

import lombok.Getter;
import org.poo.system.commerce.cashback.SpendingStrategy;
import org.poo.system.user.Account;

public abstract class ServicePlan {

    @Getter
    enum Tier {
        STANDARD(100, 350),
        STUDENT(100, 350),
        SILVER(-1, 250),
        GOLD(-1, -1);

        private final double silverUpgrade;
        private final double goldUpgrade;

        Tier(
                final double silverUpgrade,
                final double goldUpgrade
        ) {
            this.silverUpgrade = silverUpgrade;
            this.goldUpgrade = goldUpgrade;
        }

    }

    private final Tier tier;

    protected ServicePlan(final Tier tier) {
        this.tier = tier;
    }

    /**
     * Applies a transactional fee
     *
     * @param account the account on which to apply the fee
     * @param amount the transaction amount
     */
    public abstract void applyFee(Account account, double amount);

    /**
     * Applies a spending cashback on the given account
     *
     * @param account the account to add the cashback on
     * @param spendingTier the spending tier reached
     * @param amount the transaction amount in RON
     */
    public abstract void applySpendingCashback(
            Account account,
            SpendingStrategy.Tier spendingTier,
            double amount
    );


}
