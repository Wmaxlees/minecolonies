package com.minecolonies.api.tileentities;

import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.Tuple;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractTileEntityWareHouse extends TileEntityColonyBuilding
{
    public AbstractTileEntityWareHouse(final BlockEntityType<? extends AbstractTileEntityWareHouse> warehouse, final BlockPos pos, final BlockState state)
    {
        super(warehouse, pos, state);
    }

    /**
     * Dump the inventory of a citizen into the warehouse. Go through all items and search the right chest to dump it in.
     *
     * @param inventoryCitizen the inventory of the citizen
     */
    public abstract void dumpInventoryIntoWareHouse(@NotNull InventoryCitizen inventoryCitizen);
}
