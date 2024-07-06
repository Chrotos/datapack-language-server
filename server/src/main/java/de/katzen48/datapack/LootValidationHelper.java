package de.katzen48.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.util.concurrent.atomic.AtomicReference;
import java.util.Optional;
import java.util.HashMap;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URLDecoder;

public class LootValidationHelper {
    private final Gson gson = new GsonBuilder().create();
    private final ReflectionHelper reflectionHelper;

    public LootValidationHelper(ReflectionHelper reflectionHelper) {
        this.reflectionHelper = reflectionHelper;
    }

    public HashMap<String, String> validateLootData(String rootDir) {
        HashMap<String, String> lootDataErrors = new HashMap<>();

        if (reflectionHelper.getLootDataTypeProxy() == null) {
            return lootDataErrors;
        }
        
        reflectionHelper.getLootDataTypeProxy().values().forEach(lootDataType -> {
            String directory = reflectionHelper.getLootDataTypeDirectory(lootDataType);
            Path path = Path.of(rootDir, directory);

            try {
                Files.walk(path).forEach(file -> {
                    Optional<String> error = validateLootData(lootDataType, file, "");
                    if (error.isPresent()) {
                        lootDataErrors.put(file.toString(), error.get());
                    }
                });
            } catch (Exception ignored) {}
        });

        return lootDataErrors;
    }

    public Optional<String> validateLootData(Object lootDataType, Path lootTablePath, String text) {
        if (!isValidationSupported()) {
            return Optional.empty();
        }

        try {
            JsonElement lootTable;
            if (text == null || text.isBlank()) {
                FileReader fileReader = new FileReader(lootTablePath.toFile());
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

            Object codec = reflectionHelper.getLootDataTypeProxy().codec(lootDataType);

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

    public Optional<String> validateLootDataForFile(String rootDir, String uri, String text) {
        Path path = Path.of(URI.create(uri));
        
        Object type = getType(rootDir, path);
        if (type == null) {
            return Optional.empty();
        }

        return validateLootData(type, path, text);
    }

    public Object getType(String rootDir, Path path) {
        Path dataPath = Path.of(rootDir, "data");

        Optional<?> type = reflectionHelper.getLootDataTypeProxy().values().filter(currentType -> {
            try {
                return Files.walk(dataPath).anyMatch(namespacePath -> {
                    Path typePath = namespacePath.resolve(reflectionHelper.getLootDataTypeDirectory(currentType));
    
                    return path.startsWith(typePath);
                });
            } catch (Exception ignored) {
                return false;
            }
        }).findFirst();

        return type.orElseGet(() -> null);
    }

    private boolean isValidationSupported() {
        if (reflectionHelper.getMinecraftServerProxy() == null) {
            return false;
        }

        if (reflectionHelper.getRegistryLayerProxy() == null) {
            return false;
        }

        if (reflectionHelper.getLayeredRegistryAccessProxy() == null) {
            return false;
        }

        if (reflectionHelper.getJsonOpsProxy() == null) {
            return false;
        }

        if (reflectionHelper.getEmptyTagLookupWrapperProxy() == null) {
            return false;
        }

        if (reflectionHelper.getHolderLookupProviderProxy() == null) {
            return false;
        }

        if (reflectionHelper.getDecoderProxy() == null) {
            return false;
        }

        if (reflectionHelper.getDataResultProxy() == null) {
            return false;
        }

        if (reflectionHelper.getDataResultErrorProxy() == null) {
            return false;
        }

        return true;
    }
}
