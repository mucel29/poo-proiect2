package org.poo.system.command;

import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;

public class PrintUsersCommand extends Command.Base {

    public PrintUsersCommand() {
        super(Command.Type.PRINT_USERS);
    }

    /**
     */
    @Override
    public void execute() {
        super.outputArray(
                arr -> BankingSystem
                        .getInstance()
                        .getUsers()
                        .forEach(user -> arr.add(user.toNode()))
        );
    }

}
