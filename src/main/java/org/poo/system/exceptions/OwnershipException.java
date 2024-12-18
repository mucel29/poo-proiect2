package org.poo.system.exceptions;

import org.poo.system.BankingSystem;

public final class OwnershipException extends BankingException {
    public OwnershipException(final String message) {
        super(message);
    }

    @Override
    public boolean handle() {
        return !BankingSystem.VERBOSE_LOGGING;
    }

}
