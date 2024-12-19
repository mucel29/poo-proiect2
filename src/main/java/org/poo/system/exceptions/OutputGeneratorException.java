package org.poo.system.exceptions;

import org.poo.system.exceptions.handlers.ExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OutputGeneratorException extends BankingException {

    protected final List<ExceptionHandler> handlers = new ArrayList<>();
    protected final String detailedMessage;

    public OutputGeneratorException(
            final String message,
            final String detailedMessage,
            final ExceptionHandler... handlers
    ) {
        super(message);
        this.detailedMessage = detailedMessage;
        this.handlers.addAll(Arrays.asList(handlers));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean handle() {
        handlers.forEach(h -> h.handle(super.getMessage()));
        return !handlers.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDetailedMessage() {
        if (detailedMessage != null && !detailedMessage.isEmpty()) {
            return detailedMessage;
        }

        return super.getMessage();
    }

}
