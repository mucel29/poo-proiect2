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

    // Used when instantiating the command for a silver -> gold upgrade
    private final boolean waiveFee;

    public UpgradePlanCommand(
            final String account,
            final ServicePlan.Tier newTier
    ) {
        super(Command.Type.UPGRADE_PLAN);
        this.account = account;
        this.newTier = newTier;
        this.waiveFee = false;
    }

    // Used when instantiating the command for a silver -> gold upgrade
    public UpgradePlanCommand(
            final String account,
            final ServicePlan.Tier newTier,
            final int timestamp,
            final boolean waiveFee
    ) {
        super(Command.Type.UPGRADE_PLAN);
        this.account = account;
        this.newTier = newTier;
        this.timestamp = timestamp;
        this.waiveFee = waiveFee;
    }

    /**
     * {@inheritDoc}
     *
     * @throws OwnershipException if no user owns the given account
     * @throws OperationException
     * <ul>
     *     <li>if the account owner already has the given tier</li>
     *     <li>if the account owner wants to downgrade their plan</li>
     *     <li>if the account doesn't have enough funds to upgrade</li>
     * </ul>
     */
    @Override
    public void execute() throws OwnershipException, OperationException {
        // Retrieve the account
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

        // Get the owner's plan
        ServicePlan currentPlan = targetAccount.getOwner().getServicePlan();

        // Check that the upgrade isn't to the current tier
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
            // Get the upgrade fee (throws an exception if the new tier is lower)
            upgradeFee = currentPlan.getUpgradeFee(newTier);
        } catch (OperationException e) {
            throw new OperationException(
                    "You cannot downgrade your plan.",
                    e.getMessage(),
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        // Convert the fee into the account's currency
        upgradeFee = upgradeFee.to(targetAccount.getCurrency());

        try {
            // Pay the upgrade fee only if it wasn't an automatic upgrade
            if (!waiveFee) {
                targetAccount.authorizeSpending(targetAccount.getOwner(), upgradeFee);
            }
        } catch (OperationException e) {
            throw new OperationException(
                    "Insufficient funds",
                    "upgradePlan: Insufficient funds",
                    new TransactionHandler(targetAccount, timestamp)
            );
        }

        BankingSystem.log(
                "Upgraded " + targetAccount.getOwner().getEmail()
                        + " to " + newTier.toString()
        );

        // Emmit upgrade transaction
        targetAccount.getTransactions().add(
                new Transaction.PlanUpgrade("Upgrade plan", timestamp)
                        .setNewPlanType(newTier.toString())
                        .setAccountIBAN(account)
        );

        // Update the owner's plan
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
