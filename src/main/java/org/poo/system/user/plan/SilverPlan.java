package org.poo.system.user.plan;

import org.poo.system.BankingSystem;
import org.poo.system.commerce.cashback.SpendingStrategy;
import org.poo.system.user.Account;

import java.util.Map;

public class SilverPlan extends ServicePlan {

    private static final double FEE = 1e-3;
    private static final Map<SpendingStrategy.Tier, Double> SPENDING_FEES = Map.of(
            SpendingStrategy.Tier.TIER_1, 3e-3,
            SpendingStrategy.Tier.TIER_2, 4e-3,
            SpendingStrategy.Tier.TIER_3, 5e-3,
            SpendingStrategy.Tier.TIER_0, 0.0
    );

    public SilverPlan() {
        super(Tier.SILVER);
    }

    /**
     * Applies a transactional fee
     *
     * @param account the account on which to apply the fee
     * @param amount  the transaction amount
     */
    @Override
    public void applyFee(final Account account, final double amount) {
        account.setFunds(account.getFunds() - amount * FEE);
    }

    /**
     * Applies a spending cashback on the given account
     *
     * @param account the account to add the cashback on
     * @param spendingTier the spending tier reached
     * @param amount the transaction amount in RON
     */
    @Override
    public void applySpendingCashback(
            final Account account,
            final SpendingStrategy.Tier spendingTier,
            final double amount
    ) {
        double convertedAmount =
                amount
                        * BankingSystem
                        .getExchangeProvider()
                        .getRate("RON", account.getCurrency());

        account.setFunds(account.getFunds() + convertedAmount * SPENDING_FEES.get(spendingTier));
    }
}
