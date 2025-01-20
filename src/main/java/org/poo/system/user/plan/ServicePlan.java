package org.poo.system.user.plan;

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.poo.system.BankingSystem;
import org.poo.system.command.UpgradePlanCommand;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;
import org.poo.system.user.User;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

@Getter @Builder
public final class ServicePlan {

    public enum Tier {
        STANDARD,
        STUDENT,
        SILVER,
        GOLD;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }

        /**
         * Converts a {@code String} to an {@code ServicePlan.Tier}
         * @param label the string to convert
         * @return the corresponding {@code ServicePlan.Tier}
         * @throws InputException if the label can't be converted to an {@code ServicePlan.Tier}
         */
        public static ServicePlan.Tier fromString(final String label) throws InputException {
            try {
                return Arrays
                        .stream(ServicePlan.Tier.values())
                        .filter(tier -> tier.name().equalsIgnoreCase(label))
                        .toList()
                        .getFirst();
            } catch (NoSuchElementException e) {
                throw new InputException("Unknown plan tier: " + label);
            }
        }


    }

    private final Tier tier;
    private final User subscriber;

    @Builder.Default
    private double transactionFee = 0.0;

    @Builder.Default
    private double transactionThreshold = 0.0;

    @Singular private final List<Double> spendingCashbacks;

    @Singular private final List<Double> upgradeFees;

    @Builder.Default
    private double upgradeThreshold = 0;

    @Builder.Default
    private int upgradeTransactions = 0;

    @Builder.Default
    private int upgradeProgress = 0;


    /**
     * Applies a fee on the given amount depending on the active plan
     * @param amount
     * @return the service fee
     */
   public Amount getFee(
           final Account account,
           final Amount amount
   ) {
       Amount ronAmount = amount.to("RON");

       if (this.upgradeThreshold > 0) {
           if (ronAmount.total() > this.upgradeThreshold) {
               this.upgradeProgress++;
           }

           if (
                   this.upgradeProgress >= this.upgradeTransactions
                           && this.tier.ordinal() < Tier.values().length - 1
           ) {
               new UpgradePlanCommand(
                       account.getAccountIBAN(),
                       Tier.values()[this.tier.ordinal() + 1],
                       BankingSystem.getTimestamp(),
                       true
               ).execute();
//               subscriber.setServicePlan(
//                       ServicePlanFactory.getPlan(
//                               subscriber,
//                               Tier.values()[this.tier.ordinal() + 1]
//                       )
//               );
           }
       }

        if (ronAmount.total() < transactionThreshold) {
            return new Amount(0.0, amount.currency());
        }

        Amount ronFee = new Amount(
               ronAmount.total()
                       * this.transactionFee,
               "RON"
        );


        // Update plan progress

        return ronFee.to(amount.currency());
   }

    /**
     * Applies the cashback for the given tier and active plan
     *
     * @param amount
     * @param spendingTier
     * @return the amount to be deduced by the cashback
     */
    public Amount getSpendingCashback(final Amount amount, final int spendingTier) {
        if (this.spendingCashbacks.size() <= spendingTier) {
            throw new OperationException("Unknown tier: " + spendingTier);
        }


        return new Amount(
                amount.total()
                        * this.spendingCashbacks.get(spendingTier),
                amount.currency()
        );
    }

    /**
     * Calculate the upgrade fee from the current tier to the give tier
     * @param nextTier the tier to upgrade to
     * @return the fee for upgrading to the tier in RON
     * @throws OperationException if the new plan is a downgrade
     */
    public Amount getUpgradeFee(
            final ServicePlan.Tier nextTier
    ) throws OperationException {
       if (tier.compareTo(nextTier) > 0) {
           throw new OperationException(
                   "Current plan: "
                           + tier
                           + ", New plan: "
                           + nextTier
           );
       }

       return new Amount(upgradeFees.get(nextTier.ordinal()), "RON");
    }

}
