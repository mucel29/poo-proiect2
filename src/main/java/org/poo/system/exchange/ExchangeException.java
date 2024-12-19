package org.poo.system.exchange;

import org.poo.system.exceptions.BankingException;

public final class ExchangeException extends BankingException {
    public ExchangeException(final String message) {
        super(message);
    }

}
