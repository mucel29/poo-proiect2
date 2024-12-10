package org.poo.utils;

import org.poo.system.BankingSystem;
import org.poo.system.command.Command;
import org.poo.system.user.Account;

import java.util.List;

public class Constants {

    private Constants() {}

    public static List<String> SUPPORTED_ACCOUNT_TYPES = Account.Type.possibleValues();

    public static List<String> SUPPORTED_COMMANDS = Command.Type.possibleValues();

    public static List<String> SUPPORTED_CURRENCIES = BankingSystem.Currency.possibleValues();

}
