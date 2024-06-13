package de.katzen48.datapack.converters;

import java.util.ArrayList;

import de.katzen48.datapack.ReflectionHelper;

public class ConverterHelper {
    public static final ArrayList<ICommandConverter> commandConverters = new ArrayList<>();

    public static String convertCommand(String command, ReflectionHelper reflectionHelper) {
        String convertedCommand = command;
        for (ICommandConverter commandConverter : commandConverters) {
            convertedCommand = commandConverter.convertCommand(convertedCommand);
        }

        return convertedCommand;
    }

    static {
        // Add converters here
        try {
            Class.forName("de.katzen48.datapack.converters.Version_1_20_5.ItemComponentConverter");
        } catch (Exception ignored) {}
    }
}
