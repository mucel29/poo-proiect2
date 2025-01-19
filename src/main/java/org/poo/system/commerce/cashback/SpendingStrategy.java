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

    public SpendingStrategy(final Commerciant.Type commerciantType) {
        super(commerciantType);
    }

    /**
     * Applies a cashback based on the total spent to a certain Commerciant
     * {@inheritDoc}
     */
    @Override
    public Amount apply(final Account account, final Amount amount) {
        CommerciantData data = super.getCommerciantData(account);

        data.setSpending(
                data.getSpending()
                        + amount.to("RON").total()
        );
        data.setTransactionCount(data.getTransactionCount() + 1);



        Amount cashback = account.getOwner()
                .getServicePlan()
                .getSpendingCashback(
                        amount,
                        Tier.getTier(data.getSpending())
                );

        BankingSystem.log(
                "Total spending: " + data.getSpending() + " RON"
        );

        BankingSystem.log(
                "Applying spending cashback of tier "
                        + Tier.getTier(data.getSpending())
                        + " to "
                        + account.getAccountIBAN()
                        + " [" + data.getType() + "]"
                        + " [" + cashback + "]"
        );

        return cashback;
    }
}
