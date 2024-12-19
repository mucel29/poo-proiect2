package org.poo.system.exceptions;

import org.poo.system.exceptions.handlers.ExceptionHandler;

public final class OwnershipException extends OutputGeneratorException {


    public OwnershipException(
            final String message,
            final String detailedMessage,
            final ExceptionHandler... handlers
    ) {
        super(message, detailedMessage, handlers);
    }

    public OwnershipException(final String message) {
        super(message, null);
    }

}
