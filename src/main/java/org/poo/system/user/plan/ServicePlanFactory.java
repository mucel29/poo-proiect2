package org.poo.system.user.plan;

import org.poo.system.user.User;

import java.util.List;
import java.util.Map;

public final class ServicePlanFactory {

    private ServicePlanFactory() { }

    private static final Map<ServicePlan.Tier, Double> FEES = Map.of(
            ServicePlan.Tier.STANDARD, 2e-3,
            ServicePlan.Tier.STUDENT, 0.0,
            ServicePlan.Tier.SILVER, 1e-3,
            ServicePlan.Tier.GOLD, 0.0
    );

    private static final Map<ServicePlan.Tier, List<Double>> CASHBACKS = Map.of(
            ServicePlan.Tier.STANDARD, List.of(0.0, 1e-3, 2e-3, 25e-4),
            ServicePlan.Tier.STUDENT,
            List.of(0.0, 1e-3, 2e-3, 25e-4),
            ServicePlan.Tier.SILVER,
            List.of(0.0, 3e-3, 4e-3, 5e-3),
            ServicePlan.Tier.GOLD,
            List.of(0.0, 5e-3, 55e-4, 7e-3)
    );

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

    private static final int SILVER_UPGRADE_TRANSACTIONS = 5;
    private static final double SILVER_UPGRADE_THRESHOLD = 300.0;
    private static final double SILVER_TRANSACTION_THRESHOLD = 500.0;

    /**
     * Creates a new plan for the given user with the given tier
     *
     * @param subscriber
     * @param tier
     * @return the newly created plan
     */
    public static ServicePlan getPlan(
            final User subscriber,
            final ServicePlan.Tier tier
    ) {
        return switch (tier) {
            case STANDARD, STUDENT, GOLD ->
                ServicePlan.builder()
                        .subscriber(subscriber)
                        .tier(tier)
                        .transactionFee(FEES.get(tier))
                        .spendingCashbacks(CASHBACKS.get(tier))
                        .upgradeFees(UPGRADE_FEES.get(tier))
                        .build();
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
