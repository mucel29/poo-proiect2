package org.poo.system.command;

import com.fasterxml.jackson.databind.JsonNode;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;

public class SetAliasCommand extends Command.Base {

    private final String alias;
    private final String email;
    private final String IBAN;

    public SetAliasCommand(String alias, String email, String IBAN) {
        super(Command.Type.SET_ALIAS);
        this.alias = alias;
        this.email = email;
        this.IBAN = IBAN;
    }

    @Override
    public void execute() {
        // What can I do with the email field????
        BankingSystem.getInstance().getAliasMap().put(alias, IBAN);
    }

    public static SetAliasCommand fromNode(final JsonNode node) throws BankingInputException {
        String alias = node.get("alias").asText();
        String email = node.get("email").asText();
        String IBAN = node.get("account").asText();

        if (alias.isEmpty() || email.isEmpty() || IBAN.isEmpty()) {
            throw new BankingInputException("Missing arguments for SetAlias\n" + node.toPrettyString());
        }

        return new SetAliasCommand(alias, email, IBAN);
    }

}
