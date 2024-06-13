package de.katzen48.datapack.converters.Version_1_20_5;

import ca.spottedleaf.dataconverter.util.CommandArgumentUpgrader;
import de.katzen48.datapack.ReflectionHelper;

public class ItemComponentConverter {
    private ReflectionHelper reflectionHelper;
    private CommandArgumentUpgrader commandArgumentUpgrader = CommandArgumentUpgrader.upgrader_1_20_4_to_1_20_5(999);
    
    public ItemComponentConverter(ReflectionHelper reflectionHelper) {
        this.reflectionHelper = reflectionHelper;
    }

    public String convertItemCommand(String command) {
        return this.commandArgumentUpgrader.upgradeCommandArguments(command, command.startsWith("/"));
    }
}