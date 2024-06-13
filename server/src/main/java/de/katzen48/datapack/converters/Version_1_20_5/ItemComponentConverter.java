package de.katzen48.datapack.converters.Version_1_20_5;

import ca.spottedleaf.dataconverter.util.CommandArgumentUpgrader;
import de.katzen48.datapack.converters.ConverterHelper;
import de.katzen48.datapack.converters.ICommandConverter;

public class ItemComponentConverter implements ICommandConverter {
    private CommandArgumentUpgrader commandArgumentUpgrader = CommandArgumentUpgrader.upgrader_1_20_4_to_1_20_5(999);

    public String convertCommand(String command) {
        return this.commandArgumentUpgrader.upgradeCommandArguments(command, command.startsWith("/"));
    }

    static {
        ConverterHelper.commandConverters.add(new ItemComponentConverter());
    }
}