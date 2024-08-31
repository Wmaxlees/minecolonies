package com.minecolonies.core.recipes;

import com.minecolonies.api.crafting.IGenericRecipe;
import com.minecolonies.api.crafting.ModCraftingTypes;
import com.minecolonies.api.crafting.registry.CraftingType;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A crafting type for player defined recipes
 */
public class PlayerDefinedCraftingType extends CraftingType {
    public PlayerDefinedCraftingType()
    {
        super(ModCraftingTypes.PLAYER_DEFINED_ID);
    }

    @Override
    @NotNull
    public List<IGenericRecipe> findRecipes(@NotNull RecipeManager recipeManager, @Nullable Level world)
    {
        // Intentionally blank. Recipes are defined by the player.
        return new ArrayList<>();
    }
}
