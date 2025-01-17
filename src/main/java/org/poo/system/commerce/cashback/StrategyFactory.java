package org.poo.system.commerce.cashback;

import org.poo.system.commerce.Commerciant;

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

}
