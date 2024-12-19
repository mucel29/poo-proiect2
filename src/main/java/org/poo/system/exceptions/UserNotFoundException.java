package org.poo.system.exceptions;

public final class UserNotFoundException extends BankingException {
    public UserNotFoundException(final String message) {
        super(message);
    }

}
