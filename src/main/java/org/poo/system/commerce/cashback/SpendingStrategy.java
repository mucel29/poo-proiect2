package org.poo.system.commerce.cashback;

import lombok.Getter;
import org.poo.system.commerce.Commerciant;
import org.poo.system.user.Account;

public final class SpendingStrategy extends CommerciantStrategy.Base {

    @Getter
    public enum Tier {
        TIER_3(500),
        TIER_2(300),
        TIER_1(100),
        TIER_0(0);

        private final int threshold;

        Tier(final int threshold) {
            this.threshold = threshold;
        }

        /**
         * Computes the current spending tier
         *
         * @param amount the spent amount
         * @return the corresponding tier
         */
        public static Tier getTier(final double amount) {
            for (Tier tier : Tier.values()) {
                if (amount >= tier.threshold) {
                    return tier;
                }
            }

            return TIER_0;
        }

    }

    public SpendingStrategy(final Commerciant.Type commerciantType) {
        super(commerciantType);
    }


    /**
     * Applies the strategy on the given account
     *
     * @param account the account on which to apply the strategy on
     * @param amount  the amount to be paid in RON
     */
    @Override
    public void apply(final Account account, final double amount) {
        CommerciantData data = super.getCommerciantData(account);
        data.setSpending(data.getSpending() + amount);
        data.setTransactionCount(data.getTransactionCount() + 1);

        account.getOwner()
                .getServicePlan()
                .applySpendingCashback(
                        account,
                        Tier.getTier(data.getSpending()),
                        amount
                );

    }
}
