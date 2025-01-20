package org.poo.system.commerce.cashback;

import lombok.Getter;
import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.exchange.Amount;
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
         * @param amount the spent total
         * @return the corresponding tier
         */
        public static int getTier(final double amount) {
            for (Tier tier : Tier.values()) {
                if (amount >= tier.threshold) {
                    return Tier.values().length - 1 - tier.ordinal();
                }
            }

            return 0;
        }

    }

    public SpendingStrategy(final Commerciant commerciant) {
        super(commerciant);
    }

    /**
     * {@inheritDoc}
     *
     * Applies a cashback based on the total spent to a certain Commerciant
     */
    @Override
    public Amount apply(final Account account, final Amount amount) {
        // Record the amount spent to the commerciant
        commerciant.addSpending(account, amount);

        // Calculate the cashback to be applied using
        // The total spent to `spendingThreshold` commerciants
        // To compute the cashback tier
        Amount cashback = account.getOwner()
                .getServicePlan()
                .getSpendingCashback(
                        amount,
                        Tier.getTier(getTotalSpending(account))
                );

        BankingSystem.log(
                "Total spending: " + getTotalSpending(account) + " RON"
        );

        BankingSystem.log(
                "Applying spending cashback of tier "
                        + Tier.getTier(getTotalSpending(account))
                        + " to "
                        + account.getAccountIBAN()
                        + " [" + commerciant.getType() + "]"
                        + " [" + cashback + "]"
        );

        // Apply coupon (if any)
        return cashback.add(super.applyCoupon(account, amount));
    }
}
