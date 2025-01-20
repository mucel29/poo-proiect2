package org.poo.system.commerce.cashback;

import org.poo.system.BankingSystem;
import org.poo.system.commerce.Commerciant;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

public final class TransactionStrategy extends CommerciantStrategy.Base {

    public TransactionStrategy(final Commerciant commerciant) {
        super(commerciant);
    }

    /**
     * Applies a cashback based on the number of transactions made to a category
     * {@inheritDoc}
     */
    @Override
    public Amount apply(
            final Account account,
            final Amount amount
    ) {
        CommerciantData data = super.getCommerciantData(account);

//        data.setTransactionCount(data.getTransactionCount() + 1);
//        if (BankingSystem.getTimestamp() == 122) {
//            System.out.println("576349");
//        }

        commerciant.addTransaction(account);

        BankingSystem.log(
                account.getAccountIBAN()
                + " ["
                + commerciant.getTransactionCount(account)
                + " / "
                + commerciant.getType().getTransactionThreshold()
                + "]"
        );

        if (
                commerciant.getTransactionCount(account)
                        == commerciant.getType().getTransactionThreshold()
        ) {

            if (account.getDiscounts().containsKey(commerciant.getType())) {
                account.getDiscounts().put(commerciant.getType(), true);
            }
            return new Amount(0, amount.currency());
        }

            if (
                    account.getDiscounts().containsKey(commerciant.getType())
                            && account.getDiscounts().get(commerciant.getType())
            ) {
                account.getDiscounts().remove(commerciant.getType());

                Amount cashback = new Amount(
                        amount.total()
                                * commerciant.getType().getTransactionCashback(),
                        amount.currency()
                );

                BankingSystem.log(
                        "Applied transaction cashback to "
                                + account.getAccountIBAN()
                                + " ["
                                + cashback
                                + "]"
                );

                return cashback;
            }




        return new Amount(0, amount.currency());
    }
}
