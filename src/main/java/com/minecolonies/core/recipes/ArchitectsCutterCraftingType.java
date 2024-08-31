package com.minecolonies.core.recipes;

import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlock;
import com.ldtteam.domumornamentum.block.IMateriallyTexturedBlockComponent;
import com.ldtteam.domumornamentum.recipe.ModRecipeTypes;
import com.ldtteam.domumornamentum.recipe.architectscutter.ArchitectsCutterRecipe;
import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.GenericRecipe;
import com.minecolonies.api.crafting.IGenericRecipe;
import com.minecolonies.api.crafting.ModCraftingTypes;
import com.minecolonies.api.crafting.RecipeCraftingType;
import com.minecolonies.api.inventory.container.ContainerCrafting;
import com.minecolonies.api.util.constant.ToolType;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ArchitectsCutterCraftingType extends RecipeCraftingType<Container, ArchitectsCutterRecipe>
{
    public ArchitectsCutterCraftingType()
    {
        super(ModCraftingTypes.ARCHITECTS_CUTTER_ID, ModRecipeTypes.ARCHITECTS_CUTTER.get(), null);
    }

    @Override
    public @NotNull List<IGenericRecipe> findRecipes(@NotNull RecipeManager recipeManager, @Nullable Level world)
    {
        final Random rnd = new Random();
        final List<IGenericRecipe> recipes = new ArrayList<>();
        for (final ArchitectsCutterRecipe recipe : recipeManager.getAllRecipesFor(ModRecipeTypes.ARCHITECTS_CUTTER.get()))
        {
            // cutter recipes don't implement getIngredients(), so we have to work around it
            final Block generatedBlock = ForgeRegistries.BLOCKS.getValue(recipe.getBlockName());

            if (!(generatedBlock instanceof final IMateriallyTexturedBlock materiallyTexturedBlock))
                continue;

            final List<List<ItemStack>> inputs = new ArrayList<>();
            for (final IMateriallyTexturedBlockComponent component : materiallyTexturedBlock.getComponents())
            {
                final List<Block> blocks = ForgeRegistries.BLOCKS.tags().getTag(component.getValidSkins()).stream()
                        .collect(Collectors.toCollection(ArrayList::new));
                Collections.shuffle(blocks, rnd);
                inputs.add(blocks.stream().map(ItemStack::new).collect(Collectors.toList()));
            }

            final ItemStack output = recipe.getResultItem(world.registryAccess()).copy();
            output.setCount(Math.max(recipe.getCount(), inputs.size()));

            // resultItem usually doesn't have textureData, but we need it to properly match the creative tab
            if (!output.getOrCreateTag().contains("textureData"))
            {
                assert output.getTag() != null;
                output.getTag().put("textureData", new CompoundTag());
            }

            recipes.add(new GenericRecipe(recipe.getId(), output, new ArrayList<>(),
                    inputs, 3, Blocks.AIR, null, ToolType.NONE, new ArrayList<>(), -1));
        }

        return recipes;
    }

    // ArchitectsCutter doesn't use the standard OpenCraftingGUIMessage
    // so we don't need this
    @NotNull
    @Override
    public MenuProvider getMenuProvider(IBuilding building, AbstractCraftingBuildingModule module) {
        return new MenuProvider()
        {
            @Override
            public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
                return null;
            }

            @Override
            public Component getDisplayName() {
                return null;
            }
        };
    }

    // ArchitectsCutter doesn't use the standard OpenCraftingGUIMessage
    // so we don't need this
    @NotNull
    @Override
    public Consumer<FriendlyByteBuf> populateMenuBuffer(IBuilding building, AbstractCraftingBuildingModule module) {
        return FriendlyByteBuf::new;
    }
}
