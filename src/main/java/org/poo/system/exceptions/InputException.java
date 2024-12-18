package org.poo.system.exceptions;

import org.poo.system.BankingSystem;

public final class InputException extends BankingException {
    public InputException(final String message) {
        super(message);
    }

    @Override
    public boolean handle() {
        return !BankingSystem.VERBOSE_LOGGING;
    }

}
