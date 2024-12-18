package org.poo.system.exceptions;

import org.poo.system.BankingSystem;

public final class AliasException extends BankingException {
    public AliasException(final String message) {
        super(message);
    }

    @Override
    public boolean handle() {
        return !BankingSystem.VERBOSE_LOGGING;
    }
}
