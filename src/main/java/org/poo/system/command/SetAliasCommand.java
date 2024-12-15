package org.poo.system.command;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import org.poo.io.IOUtils;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.exceptions.BankingInputException;

@Setter
public class SetAliasCommand extends Command.Base {

    private String alias;
    private String email;
    private String account;

    public SetAliasCommand(String alias, String email, String account) {
        super(Command.Type.SET_ALIAS);
        this.alias = alias;
        this.email = email;
        this.account = account;
    }

    public SetAliasCommand() {
        super(Command.Type.SET_ALIAS);
    }

    @Override
    public void execute() {
        // What can I do with the email field????
        BankingSystem.getInstance().getAliasMap().put(alias, account);
    }

    public static SetAliasCommand fromNode(final JsonNode node) throws BankingInputException {
        String alias = IOUtils.readStringChecked(node, "alias");
        String email = IOUtils.readStringChecked(node, "email");
        String IBAN = IOUtils.readStringChecked(node, "IBAN");

        return new SetAliasCommand(alias, email, IBAN);
    }

}
