package de.katzen48.datapack;

import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public class RecipeValidationHelper {
    private final Gson gson = new GsonBuilder().create();
    private final ReflectionHelper reflectionHelper;

    public RecipeValidationHelper(ReflectionHelper reflectionHelper) {
        this.reflectionHelper = reflectionHelper;
    }

    public Optional<String> validate(Path recipePath, String text) {
        try {
            JsonElement lootTable;
            if (text == null || text.isBlank()) {
                FileReader fileReader = new FileReader(recipePath.toFile());
                lootTable = gson.fromJson(fileReader, JsonElement.class);
            } else {
                lootTable = gson.fromJson(text, JsonElement.class);
            }

            Object registries = reflectionHelper.getMinecraftServerProxy().getRegistries(reflectionHelper.getMinecraftServerProxy().getServer());
            Object registryLayer = reflectionHelper.getRegistryLayerProxy().valueOf("RELOADABLE");
            Object frozen = reflectionHelper.getLayeredRegistryAccessProxy().getAccessForLoading(registries, registryLayer);
            Object jsonOps = reflectionHelper.getJsonOpsProxy().getInstance();

            Object emptyTagLookupWrapper = reflectionHelper.getEmptyTagLookupWrapperProxy().construct(frozen);
            Object dynamicOpsJson = reflectionHelper.getHolderLookupProviderProxy().createSerializationContext(emptyTagLookupWrapper, jsonOps);

            Object codec = reflectionHelper.getRecipeProxy().codec();
            Object dataResult = reflectionHelper.getDecoderProxy().parse(codec, dynamicOpsJson, lootTable);

            Optional<Object> errorOptional = reflectionHelper.getDataResultProxy().error(dataResult);

            if (!errorOptional.isPresent()) {
                return Optional.empty();
            }

            return errorOptional.map(error -> {
                return reflectionHelper.getDataResultErrorProxy().message(error);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    public boolean isRecipe(String rootDir, Path path) {
        try {
            Path dataPath = Path.of(rootDir, "data");
    
            return Files.walk(dataPath).anyMatch(namespacePath -> {
                Path recipePath = namespacePath.resolve("recipes");
    
                return path.startsWith(recipePath);
            });
        } catch (Exception ignored) {
            return false;
        }
    }
}
