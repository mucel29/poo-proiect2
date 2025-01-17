package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.commerce.cashback.StrategyFactory;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.user.Account;
import org.poo.system.user.plan.ServicePlan;

public class UpgradePlanCommand extends Command.Base {

    private final String account;
    private final ServicePlan.Tier newTier;
    private final boolean chargeUser;

    public UpgradePlanCommand(
            final String account,
            final ServicePlan.Tier newTier
    ) {
        super(Command.Type.UPGRADE_PLAN);
        this.account = account;
        this.newTier = newTier;
        this.chargeUser = true;
    }

    public UpgradePlanCommand(
            final String account,
            final ServicePlan.Tier newTier,
            final boolean chargeUser
    ) {
        super(Command.Type.UPGRADE_PLAN);
        this.account = account;
        this.newTier = newTier;
        this.chargeUser = chargeUser;
    }

    /**
     * Executes the command instance. May produce transactions or errors.
     */
    @Override
    public void execute() {
        Account targetAccount;

        try {
            targetAccount = BankingSystem.getStorageProvider().getAccountByIban(account);
        } catch (OwnershipException e) {
            throw new OwnershipException(
                    "Account not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        }

        ServicePlan.Tier currentTier = targetAccount.getOwner().getServicePlan().getTier();

        if (currentTier == newTier) {
            throw new OperationException(
                    "The user already has the "
                        + newTier
                        + " plan.",
                    "The user already has the "
                        + newTier
                        + " plan.",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        if (currentTier.compareTo(newTier) > 0) {
            throw new OperationException(
                    "You cannot downgrade your plan.",
                    "Current plan: " + currentTier
                        + ", New plan: " + newTier,
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        double upgradeFee = switch (newTier) {
            case SILVER -> currentTier.getSilverUpgrade();
            case GOLD -> currentTier.getGoldUpgrade();
            default -> -1;
        };

        upgradeFee *= BankingSystem.getExchangeProvider().getRate(
                "RON", targetAccount.getCurrency()
        );

        if (chargeUser && upgradeFee > targetAccount.getFunds()) {
            throw new OperationException(
                    "Insufficient funds",
                    "upgradePlan: Insufficient funds",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        double oldBal = targetAccount.getFunds();

        if (chargeUser) {
            targetAccount.setFunds(targetAccount.getFunds() - upgradeFee);
        }

        BankingSystem.log(
                "Upgraded " + targetAccount.getOwner().getEmail()
                        + " to " + newTier.toString()
                        + " [" + oldBal + " -> " + targetAccount.getFunds() + "]"
        );

        targetAccount.getTransactions().add(
                new Transaction.PlanUpgrade("Upgrade plan", timestamp)
                        .setNewPlanType(newTier.toString())
                        .setAccountIBAN(account)
        );



        targetAccount.getOwner().setServicePlan(
                StrategyFactory.getServicePlan(newTier)
        );

    }

    /**
     * Deserializes the given node into a {@code Command.Base}  instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        ServicePlan.Tier newTier = ServicePlan.Tier.fromString(
                IOUtils.readStringChecked(node, "newPlanType")
        );

        return new UpgradePlanCommand(account, newTier);
    }

}
