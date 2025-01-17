package org.poo.system.exceptions;

import org.poo.system.exceptions.handlers.ExceptionHandler;

public final class UserNotFoundException extends OutputGeneratorException {
    public UserNotFoundException(
            final String message,
            final String detailedMessage,
            final ExceptionHandler... handlers
    ) {
        super(message, detailedMessage, handlers);
    }

    public UserNotFoundException(
            final String message
    ) {
        super(message, null);
    }

}
