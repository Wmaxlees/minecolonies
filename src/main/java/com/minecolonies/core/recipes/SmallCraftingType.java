package com.minecolonies.core.recipes;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.crafting.ModCraftingTypes;
import com.minecolonies.api.crafting.RecipeCraftingType;
import com.minecolonies.api.inventory.container.ContainerCrafting;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class SmallCraftingType extends RecipeCraftingType {
    public SmallCraftingType() {
        super(ModCraftingTypes.SMALL_CRAFTING_ID,
                RecipeType.CRAFTING, predicate());
    }

    private static Predicate<Recipe> predicate() {
        return r -> r.canCraftInDimensions(2, 2);
    }

    @NotNull
    @Override
    public MenuProvider getMenuProvider(IBuilding building, AbstractCraftingBuildingModule module) {
        return new MenuProvider()
        {
            @NotNull
            @Override
            public Component getDisplayName()
            {
                return Component.literal("Crafting GUI");
            }

            @NotNull
            @Override
            public AbstractContainerMenu createMenu(final int id, @NotNull final Inventory inv, @NotNull final Player player)
            {
                return new ContainerCrafting(id, inv, false, building.getID(), module.getProducer().getRuntimeID());
            }
        };
    }

    @NotNull
    @Override
    public Consumer<FriendlyByteBuf> populateMenuBuffer(IBuilding building, AbstractCraftingBuildingModule module) {
        return buffer -> new FriendlyByteBuf(buffer.writeBoolean(false)).writeBlockPos(building.getID())
                .writeInt(module.getProducer().getRuntimeID());
    }
}
