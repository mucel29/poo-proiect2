package org.poo.system.exceptions;

import org.poo.system.exceptions.handlers.ExceptionHandler;

public final class OperationException extends OutputGeneratorException {

    public OperationException(
            final String message,
            final String detailedMessage,
            final ExceptionHandler... handlers
    ) {
        super(message, detailedMessage, handlers);
    }

    public OperationException(
            final String message
    ) {
        super(message, null);
    }

}
