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
import org.poo.system.exceptions.handlers.CommandErrorHandler;
import org.poo.system.exceptions.handlers.TransactionHandler;
import org.poo.system.exchange.Amount;
import org.poo.system.user.Account;

import java.util.Optional;

public class WithdrawSavingsCommand extends Command.Base {

    private static final int MINIMUM_AGE = 21;

    private final String account;
    private final Amount amount;

    public WithdrawSavingsCommand(
            final String account,
            final Amount amount
    ) {
        super(Command.Type.WITHDRAW_SAVINGS);
        this.account = account;
        this.amount = amount;
    }

    /**
     * {@inheritDoc}
     * @throws OwnershipException if no user owns the given account
     * @throws OperationException
     * <ul>
     *     <li>if the account is not a savings one</li>
     *     <li>if the user isn't 21 years old</li>
     *     <li>if the user has no other classic accounts</li>
     * </ul>
     */
    @Override
    public void execute() throws OwnershipException, OperationException {

        // Retrieve the account from the storage provider
        Account savingsAccount;
        try {
            savingsAccount = BankingSystem.getStorageProvider().getAccountByIban(account);
        } catch (OwnershipException e) {
            throw new OwnershipException(
                    "Account not found",
                    e.getMessage(),
                    new CommandDescriptionHandler(this)
            );
        }

        // Check if the account is not a savings one
        if (savingsAccount.getAccountType() != Account.Type.SAVINGS) {
            throw new OperationException(
                    "Account is not of type savings.",
                    "Account " + savingsAccount.getAccountIBAN() + " is not a savings account",
                    new CommandErrorHandler(this, false)
            );
        }

        // Check if the owner meets the minimum age
        if (savingsAccount.getOwner().getAge().getYears() < MINIMUM_AGE) {
            throw new OperationException(
                    "You don't have the minimum age required.",
                    null,
                    new TransactionHandler(savingsAccount, timestamp)
            );
        }

        // Find the first classic account in the given currency
        Optional<Account> target = savingsAccount
                .getOwner()
                .getAccounts()
                .stream()
                .filter(
                        acc -> acc.getCurrency().equals(amount.currency())
                                && acc.getAccountType().equals(Account.Type.CLASSIC)
                ).findFirst();

        // Check if any account was found
        if (target.isEmpty()) {
            throw new OwnershipException(
                    "You do not have a classic account.",
                    null,
                    new TransactionHandler(savingsAccount, timestamp)
            );
        }

        // Account was found, can use get
        Account targetAccount = target.get();

        // Convert requested amount to the saving account's currency
        Amount senderAmount = amount.to(savingsAccount.getCurrency());

        // Perform the transfer
        try {
            savingsAccount.authorizeSpending(savingsAccount.getOwner(), senderAmount);
        } catch (OperationException e) {
            throw new OperationException(
                    "Insufficient funds",
                    null,
                    new TransactionHandler(savingsAccount, timestamp)
            );
        }

        // Emmit withdrawal transaction
        Transaction savingsTransaction = new Transaction.SvaingsWithdraw(
                "Savings withdrawal",
                timestamp
        )
                .setSavingsAccountIBAN(savingsAccount.getAccountIBAN())
                .setClassicAccountIBAN(targetAccount.getAccountIBAN())
                .setAmount(amount.total());


        // Deposit funds into the target
        targetAccount.authorizeDeposit(targetAccount.getOwner(), amount);

        savingsAccount.getTransactions().add(savingsTransaction);
        targetAccount.getTransactions().add(savingsTransaction);

    }

    /**
     * Deserializes the given node into a {@code Command.Base}  instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String account = IOUtils.readStringChecked(node, "account");
        double amount = IOUtils.readDoubleChecked(node, "amount");
        String currency = IOUtils.readStringChecked(node, "currency");

        return new WithdrawSavingsCommand(account, new Amount(amount, currency));
    }

}
