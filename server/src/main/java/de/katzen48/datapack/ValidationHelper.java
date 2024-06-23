package de.katzen48.datapack;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

public class ValidationHelper {
    private final LootValidationHelper lootValidationHelper;
    private final RecipeValidationHelper recipeValidationHelper;

    public ValidationHelper(LootValidationHelper lootValidationHelper, RecipeValidationHelper recipeValidationHelper) {
        this.lootValidationHelper = lootValidationHelper;
        this.recipeValidationHelper = recipeValidationHelper;
    }

    public Optional<String> validate(String rootDir, String uri, String text) {
        if (uri.endsWith(".json")) {
            Path path = Path.of(URI.create(uri));

            Object lootType = lootValidationHelper.getType(rootDir, path);
            if (lootType != null) {
                return lootValidationHelper.validateLootData(lootType, path, text);
            }

            if (recipeValidationHelper.isRecipe(rootDir, path)) {
                return recipeValidationHelper.validate(path, text);
            }
        }

        return Optional.empty();
    }
}
