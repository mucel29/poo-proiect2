package org.poo.system.commerce.cashback;

import org.poo.system.commerce.Commerciant;

public final class StrategyFactory {

    private StrategyFactory() { }

    /**
     * Creates a new {@code CommerciantStrategy}
     * @param commerciant the {@code Commerciant} using this strategy
     * @param strategyType the strategy type to use
     *
     * @return the requested strategy
     */
    public static CommerciantStrategy getCommerciantStrategy(
            final Commerciant commerciant,
            final CommerciantStrategy.Type strategyType
    ) {
        return switch (strategyType) {
            case SPENDING -> new SpendingStrategy(commerciant);
            case TRANSACTIONS -> new TransactionStrategy(commerciant);
        };
    }

}
