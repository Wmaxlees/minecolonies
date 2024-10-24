package com.minecolonies.core.tileentities;

import com.minecolonies.api.inventory.IInventory;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.tileentities.AbstractTileEntityWareHouse;
import com.minecolonies.api.tileentities.MinecoloniesTileEntities;
import com.minecolonies.api.util.*;
import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;
import com.minecolonies.core.colony.buildings.modules.BuildingModules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.minecolonies.api.util.constant.Constants.TICKS_FIVE_MIN;
import static com.minecolonies.api.util.constant.TranslationConstants.*;
import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingWareHouse.MAX_STORAGE_UPGRADE;

/**
 * Class which handles the tileEntity of our colony warehouse.
 */
public class TileEntityWareHouse extends AbstractTileEntityWareHouse
{
    /**
     * Time of last sent notifications.
     */
    private long lastNotification                   = 0;

    public TileEntityWareHouse(final BlockPos pos, final BlockState state)
    {
        super(MinecoloniesTileEntities.WAREHOUSE.get(), pos, state);
    }

    @Override
    public void dumpInventoryIntoWareHouse(@NotNull final InventoryCitizen inventoryCitizen)
    {
        final List<ItemStack> itemsToInsert = ItemStackUtils.convertToMaxSizeItemStacks(inventoryCitizen.getAllItems());
        for (final ItemStack stack : itemsToInsert)
        {
            if (ItemStackUtils.isEmpty(stack))
            {
                continue;
            }

            @Nullable final IInventory inv = getInventoryForStack(stack);
            if (inv == null)
            {
                if(level.getGameTime() - lastNotification > TICKS_FIVE_MIN)
                {
                    lastNotification = level.getGameTime();
                    if (getBuilding().getBuildingLevel() == getBuilding().getMaxBuildingLevel())
                    {
                        if (getBuilding().getModule(BuildingModules.WAREHOUSE_OPTIONS).getStorageUpgrade() < MAX_STORAGE_UPGRADE)
                        {
                            MessageUtils.format(COM_MINECOLONIES_COREMOD_WAREHOUSE_FULL_LEVEL5_UPGRADE).sendTo(getColony()).forAllPlayers();
                        }
                        else
                        {
                            MessageUtils.format(COM_MINECOLONIES_COREMOD_WAREHOUSE_FULL_MAX_UPGRADE).sendTo(getColony()).forAllPlayers();
                        }
                    }
                    else
                    {
                        MessageUtils.format(COM_MINECOLONIES_COREMOD_WAREHOUSE_FULL).sendTo(getColony()).forAllPlayers();
                    }
                }
                return;
            }

            inv.insert(stack, false);
        }
    }

    /**
     * Get a rack for a stack.
     * @param stack the stack to insert.
     * @return the matching rack.
     */
    public IInventory getInventoryForStack(final ItemStack stack)
    {
        IInventory inventory = getPositionOfInventoryWithItemStack(stack);
        if (inventory == null)
        {
            inventory = getPositionOfInventoryWithSimilarItemStack(stack);
            if (inventory == null)
            {
                inventory = searchMostEmptyInventory();
            }
        }
        return inventory;
    }

    /**
     * Search the right chest for an itemStack.
     *
     * @param stack the stack to dump.
     * @return the tile entity of the chest
     */
    @Nullable
    private IInventory getPositionOfInventoryWithItemStack(@NotNull final ItemStack stack)
    {
        final Matcher matcher = new Matcher.Builder(stack.getItem()).build();
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            if (WorldUtil.isBlockLoaded(level, pos))
            {
                final BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof IInventory inv)
                {
                    if (!inv.isFull() && inv.hasMatch(matcher))
                    {
                        return inv;
                    }
                }
            }
        }

        return null;
    }

    /**
     * Searches a chest with a similar item as the incoming stack.
     *
     * @param stack the stack.
     * @return the entity of the chest.
     */
    @Nullable
    private IInventory getPositionOfInventoryWithSimilarItemStack(final ItemStack stack)
    {
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            if (WorldUtil.isBlockLoaded(level, pos))
            {
                final BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof IInventory inv)
                {
                    if (!inv.isFull() && inv.hasSimilar(stack.getItem()))
                    {
                        return inv;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Search for the chest with the least items in it.
     *
     * @return the tileEntity of this chest.
     */
    @Nullable
    private IInventory searchMostEmptyInventory()
    {
        float percentFull = 100;
        IInventory emptiestInventory = null;
        for (@NotNull final BlockPos pos : getBuilding().getContainers())
        {
            final BlockEntity entity = getLevel().getBlockEntity(pos);
            if (entity instanceof IInventory inv)
            {
                if (inv.isEmpty())
                {
                    return inv;
                }

                final float tempPercentFull = inv.getPercentFull();
                if (tempPercentFull > percentFull)
                {
                    percentFull = tempPercentFull;
                    emptiestInventory = inv;
                }
            }
        }
        return emptiestInventory;
    }

    public BlockPos getInventoryLocation(ItemStack stack)
    {
        final Matcher matcher = new Matcher.Builder(stack.getItem())
            .compareDamage(stack.getDamageValue())
            .compareNBT(ItemNBTMatcher.EXACT_MATCH, stack.getTag())
            .compareCount(ItemCountType.MATCH_COUNT_EXACTLY, stack.getCount())
            .build();
        for (BlockPos pos : getBuilding().getContainers())
        {
            if (WorldUtil.isBlockLoaded(level, pos))
            {
                BlockEntity entity = getLevel().getBlockEntity(pos);
                if (entity instanceof IInventory inv)
                {
                    if (inv.hasMatch(matcher))
                    {
                        return pos;
                    }
                }
            }
        }

        return null;
    }
}
