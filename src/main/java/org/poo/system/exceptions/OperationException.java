package org.poo.system.exceptions;

import org.poo.system.exceptions.handlers.ExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class OperationException extends BankingException {

    private final List<ExceptionHandler> handlers = new ArrayList<>();
    private final String detailedMessage;

    public OperationException(
            final String message,
            final String detailedMessage,
            final ExceptionHandler... handlers
    ) {
        super(message);
        this.detailedMessage = detailedMessage;
        this.handlers.addAll(Arrays.asList(handlers));
    }

    @Override
    public boolean handle() {
        handlers.forEach(h -> h.handle(super.getMessage()));
        return true;
    }

    @Override
    public String getDetailedMessage() {
        if (detailedMessage != null && !detailedMessage.isEmpty()) {
            return detailedMessage;
        }

        return super.getMessage();
    }

}
