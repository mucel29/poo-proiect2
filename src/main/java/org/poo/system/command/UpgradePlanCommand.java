package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.Transaction;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;
import org.poo.system.exceptions.OperationException;
import org.poo.system.exceptions.OwnershipException;
import org.poo.system.exceptions.handlers.CommandDescriptionHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;
import org.poo.system.user.plan.ServicePlan;
import org.poo.system.user.plan.ServicePlanFactory;

public class UpgradePlanCommand extends Command.Base {

    private final String account;
    private final ServicePlan.Tier newTier;

    public UpgradePlanCommand(
            final String account,
            final ServicePlan.Tier newTier
    ) {
        super(Command.Type.UPGRADE_PLAN);
        this.account = account;
        this.newTier = newTier;
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

        ServicePlan currentPlan = targetAccount.getOwner().getServicePlan();

        if (currentPlan.getTier() == newTier) {
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


        Amount upgradeFee;
        try {
            upgradeFee = currentPlan.getUpgradeFee(newTier);
        } catch (OperationException e) {
            throw new OperationException(
                    "You cannot downgrade your plan.",
                    e.getMessage(),
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        upgradeFee = upgradeFee.to(targetAccount.getCurrency());

        if (targetAccount.getFunds().total() < upgradeFee.total()) {
            throw new OperationException(
                    "Insufficient funds",
                    "upgradePlan: Insufficient funds",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }


        targetAccount.setFunds(targetAccount.getFunds().sub(upgradeFee));

        BankingSystem.log(
                "Upgraded " + targetAccount.getOwner().getEmail()
                        + " to " + newTier.toString()
        );

        targetAccount.getTransactions().add(
                new Transaction.PlanUpgrade("Upgrade plan", timestamp)
                        .setNewPlanType(newTier.toString())
                        .setAccountIBAN(account)
        );



        targetAccount.getOwner().setServicePlan(
                ServicePlanFactory.getPlan(targetAccount.getOwner(), newTier)
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
