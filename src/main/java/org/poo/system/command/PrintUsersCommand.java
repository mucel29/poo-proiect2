package org.poo.system.command;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.io.StateWriter;
import org.poo.system.BankingSystem;
import org.poo.system.command.base.Command;
import org.poo.system.user.User;

public class PrintUsersCommand extends Command.Base {

    public PrintUsersCommand() {
        super(Type.PRINT_USERS);
    }

    @Override
    public void execute() {
        ObjectNode root = StateWriter.getMapper().createObjectNode();

        root.put("command", command.toString());
        root.put("timestamp", timestamp);

        ArrayNode output = root.putArray("output");
        for (User u : BankingSystem.getInstance().getUsers()){
            output.add(u.toNode());
        }

        StateWriter.write(root);
    }

}