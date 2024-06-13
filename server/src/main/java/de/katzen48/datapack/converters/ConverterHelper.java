package de.katzen48.datapack.converters;

import de.katzen48.datapack.ReflectionHelper;

public class ConverterHelper {
    public static boolean shouldConvertCommand1_20_5() {
        try {
            Class.forName("ca.spottedleaf.dataconverter.util.CommandArgumentUpgrader");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public static String convertCommand(String command, ReflectionHelper reflectionHelper) {
        if (shouldConvertCommand1_20_5()) {
            return new de.katzen48.datapack.converters.Version_1_20_5.ItemComponentConverter(reflectionHelper).convertItemCommand(command);
        }

        return command;
    }
}
