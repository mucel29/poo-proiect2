package org.poo.system.exceptions.handlers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.poo.io.StateWriter;
import org.poo.system.command.base.Command;

/**
 * An exception handler that outputs a command result
 * with a description containing the exception message
 */
public final class CommandDescriptionHandler implements ExceptionHandler {


    private final Command.Base command;

    public CommandDescriptionHandler(final Command.Base command) {
        this.command = command;
    }

    /**
     * Called by a BankingException if it was provided to it
     * @param message the message to display in the output
     */
    @Override
    public void handle(final String message) {
        ObjectNode node = StateWriter.getMapper().createObjectNode();
        node.put("command", command.getCommand().toString());
        node.put("timestamp", command.getTimestamp());
        ObjectNode output = node.putObject("output");
        output.put("description", message);
        output.put("timestamp", command.getTimestamp());
        StateWriter.write(node);
    }

}
