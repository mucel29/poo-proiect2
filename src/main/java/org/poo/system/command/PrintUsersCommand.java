package org.poo.system.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.user.User;

public class PrintUsersCommand extends Command.Base {

    public PrintUsersCommand() {
        super(Command.Type.PRINT_USERS);
    }

    @Override
    public void execute() {
        super.outputArray(
                arr -> BankingSystem
                        .getInstance()
                        .getUsers()
                        .stream()
                        .forEach(
                                user -> arr
                                        .add(user.toNode())
                                )
        );
    }

}