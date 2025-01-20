package org.poo.system.user.plan;

import org.poo.system.user.User;

import java.util.List;
import java.util.Map;

public final class ServicePlanFactory {

    private ServicePlanFactory() { }

    // Transaction fees for each tier
    private static final Map<ServicePlan.Tier, Double> FEES = Map.of(
            ServicePlan.Tier.STANDARD, 2e-3,
            ServicePlan.Tier.STUDENT, 0.0,
            ServicePlan.Tier.SILVER, 1e-3,
            ServicePlan.Tier.GOLD, 0.0
    );

    // Spending cashbacks for each tier
    // Order: NO_TIER, 100 RON, 300 RON, 500 RON
    private static final Map<ServicePlan.Tier, List<Double>> CASHBACKS = Map.of(
            ServicePlan.Tier.STANDARD,
            List.of(0.0, 1e-3, 2e-3, 25e-4),
            ServicePlan.Tier.STUDENT,
            List.of(0.0, 1e-3, 2e-3, 25e-4),
            ServicePlan.Tier.SILVER,
            List.of(0.0, 3e-3, 4e-3, 5e-3),
            ServicePlan.Tier.GOLD,
            List.of(0.0, 5e-3, 55e-4, 7e-3)
    );

    // Upgrade fees for each tier
    // ORDER: TO_STANDARD, TO_STUDENT, TO_SILVER, TO_GOLD
    private static final Map<ServicePlan.Tier, List<Double>> UPGRADE_FEES = Map.of(
            ServicePlan.Tier.STANDARD,
                List.of(0.0, 0.0, 100.0, 350.0),
            ServicePlan.Tier.STUDENT,
                List.of(0.0, 0.0, 100.0, 350.0),
            ServicePlan.Tier.SILVER,
                List.of(0.0, 0.0, 0.0, 250.0),
            ServicePlan.Tier.GOLD,
                List.of(0.0, 0.0, 0.0, 0.0)
    );

    // Number of transactions to get upgraded to GOLD
    private static final int SILVER_UPGRADE_TRANSACTIONS = 5;

    // Payments of at least 300 RON count towards the upgrade progress
    private static final double SILVER_UPGRADE_THRESHOLD = 300.0;

    // Transactions under 500 RON don't get fees applied to them
    private static final double SILVER_TRANSACTION_THRESHOLD = 500.0;

    /**
     * Creates a new plan for the given user with the given tier
     *
     * @param subscriber the user
     * @param tier the requested tier
     * @return the newly created plan
     */
    public static ServicePlan getPlan(
            final User subscriber,
            final ServicePlan.Tier tier
    ) {
        return switch (tier) {
            // Retrieve the values from the maps
            case STANDARD, STUDENT, GOLD ->
                ServicePlan.builder()
                        .subscriber(subscriber)
                        .tier(tier)
                        .transactionFee(FEES.get(tier))
                        .spendingCashbacks(CASHBACKS.get(tier))
                        .upgradeFees(UPGRADE_FEES.get(tier))
                        .build();
            // Also add upgrade params and fee threshold
            case SILVER ->
                ServicePlan.builder()
                        .subscriber(subscriber)
                        .tier(tier)
                        .transactionFee(FEES.get(tier))
                        .spendingCashbacks(CASHBACKS.get(tier))
                        .upgradeFees(UPGRADE_FEES.get(tier))
                        .upgradeThreshold(SILVER_UPGRADE_THRESHOLD)
                        .upgradeTransactions(SILVER_UPGRADE_TRANSACTIONS)
                        .transactionThreshold(SILVER_TRANSACTION_THRESHOLD)
                        .build();

        };
    }

}
