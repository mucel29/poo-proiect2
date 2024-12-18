package org.poo.system.exchange;

import org.poo.system.BankingSystem;
import org.poo.system.exceptions.BankingException;

public final class ExchangeException extends BankingException {
    public ExchangeException(final String message) {
        super(message);
    }

    @Override
    public boolean handle() {
        return !BankingSystem.VERBOSE_LOGGING;
    }

}
