package org.poo.system.commerce.cashback;

import org.poo.system.commerce.Commerciant;
import org.poo.system.user.plan.GoldPlan;
import org.poo.system.user.plan.ServicePlan;
import org.poo.system.user.plan.SilverPlan;
import org.poo.system.user.plan.StandardPlan;
import org.poo.system.user.plan.StudentPlan;

public final class StrategyFactory {

    private StrategyFactory() { }

    /**
     * Creates a new {@code CommerciantStrategy}
     * @param commerciantType
     * @param strategyType
     * @return the requested strategy
     */
    public static CommerciantStrategy getCommerciantStrategy(
            final Commerciant.Type commerciantType,
            final CommerciantStrategy.Type strategyType
    ) {
        return switch (strategyType) {
            case SPENDING -> new SpendingStrategy(commerciantType);
            case TRANSACTIONS -> new TransactionStrategy(commerciantType);
        };
    }

    /**
     * Creates a new service plan
     * @param tier
     * @return
     */
    public static ServicePlan getServicePlan(final ServicePlan.Tier tier) {
        return switch (tier) {
            case STANDARD -> new StandardPlan();
            case STUDENT -> new StudentPlan();
            case SILVER -> new SilverPlan();
            case GOLD -> new GoldPlan();
        };
    }

}
