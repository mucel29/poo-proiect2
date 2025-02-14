package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Setter;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.InputException;

@Setter
public class SetAliasCommand extends Command.Base {

    private final String alias;
    private final String email;
    private final String account;

    public SetAliasCommand(
            final String alias,
            final String email,
            final String account
    ) {
        super(Command.Type.SET_ALIAS);
        this.alias = alias;
        this.email = email;
        this.account = account;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        // What can I do with the email field????
        BankingSystem
                .getStorageProvider()
                .registerAlias(
                        BankingSystem.getStorageProvider().getAccountByIban(account),
                        alias
                );
    }

    /**
     * Deserializes the given node into a {@code Command.Base} instance
     * @param node the node to deserialize
     * @return the command represented by the node
     * @throws InputException if the node is not a valid command
     */
    public static Command.Base fromNode(final JsonNode node) throws InputException {
        String alias = IOUtils.readStringChecked(node, "alias");
        String email = IOUtils.readStringChecked(node, "email");
        String account = IOUtils.readStringChecked(node, "account");

        return new SetAliasCommand(alias, email, account);
    }

}
