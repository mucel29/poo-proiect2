package org.poo.system.storage;

import org.poo.system.BankingSystem;
import org.poo.system.exceptions.BankingException;

public final class StorageException extends BankingException {
    public StorageException(final String message) {
        super(message);
    }

    @Override
    public boolean handle() {
        return !BankingSystem.VERBOSE_LOGGING;
    }

}
