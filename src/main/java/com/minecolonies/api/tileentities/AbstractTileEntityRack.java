package com.minecolonies.api.tileentities;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.inventory.InventoryId;
import com.minecolonies.api.inventory.InventoryRack;
import com.minecolonies.api.inventory.events.InventoryEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

import static com.minecolonies.api.util.constant.Constants.DEFAULT_SIZE;

import javax.annotation.Nonnull;

public abstract class AbstractTileEntityRack extends InventoryRack implements MenuProvider
{
    /**
     * Pos of the owning building.
     */
    protected BlockPos buildingPos = BlockPos.ZERO;

    /**
     * Create a new rack.
     * @param tileEntityTypeIn the specific block entity type.
     * @param pos the position.
     * @param state its state.
     */
    public AbstractTileEntityRack(final BlockEntityType<?> tileEntityTypeIn, final BlockPos pos, final BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
        setInventory(createInventory(DEFAULT_SIZE));
    }

    /**
     * Create a rack with a specific inventory size.
     * @param tileEntityTypeIn the specific block entity type.
     * @param pos the position.
     * @param state its state.
     * @param size the ack size.
     */
    public AbstractTileEntityRack(final BlockEntityType<?> tileEntityTypeIn, final BlockPos pos, final BlockState state, final int size)
    {
        super(tileEntityTypeIn, pos, state);
        setInventory(createInventory(size));
    }

    public ItemStackHandler getItemStackHandler()
    {
        return getItemHandler();
    }

    /**
     * Rack inventory type.
     */
    public class RackInventory extends ItemStackHandler
    {
        public RackInventory(final int defaultSize)
        {
            super(defaultSize);
        }

        @Override
        protected void onContentsChanged(final int slot)
        {
            updateItemStorage();
            super.onContentsChanged(slot);
        }

        @Override
        public void setStackInSlot(final int slot, final @Nonnull ItemStack stack)
        {
            validateSlotIndex(slot);
            final ItemStack previous = this.stacks.get(slot);
            if (!ItemStack.matches(stack, previous))
            {
                this.stacks.set(slot, stack);
                onContentsChanged(slot);
                if (level != null && !level.isClientSide) {
                    MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(previous,
                            InventoryEvent.UpdateType.REMOVE, new InventoryId(getBlockPos()));
                    MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(stack,
                            InventoryEvent.UpdateType.ADD, new InventoryId(getBlockPos()));
                }
            }
        }

        @Nonnull
        @Override
        public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate)
        {
            final ItemStack result = super.insertItem(slot, stack, simulate);
            if ((result.isEmpty() || result.getCount() < stack.getCount()) && !simulate)
            {
                onContentsChanged(slot);
                MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(
                        stack.copyWithCount(stack.getCount() - result.getCount()),
                        InventoryEvent.UpdateType.ADD, new InventoryId(getBlockPos()));
            }
            return result;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            final ItemStack stack = super.extractItem(slot, amount, simulate);
            if (!stack.isEmpty() && !simulate)
            {
                onContentsChanged(slot);
                MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(stack,
                        InventoryEvent.UpdateType.REMOVE, new InventoryId(getBlockPos()));
            }
            return stack;
        }
    }

    /**
     * Create the inventory that belongs to the rack.
     *
     * @param slots the number of slots.
     * @return the created inventory,
     */
    public abstract ItemStackHandler createInventory(final int slots);

    /**
     * Upgrade the rack by 1. This adds 9 more slots and copies the inventory to the new one.
     */
    public abstract void upgradeRackSize();

    /**
     * Set the building pos it belongs to.
     *
     * @param pos the pos of the building.
     */
    public void setBuildingPos(final BlockPos pos)
    {
        if (level != null && (buildingPos == null || !buildingPos.equals(pos)))
        {
            setChanged();
        }
        this.buildingPos = pos;
    }

    /**
     * Get the upgrade size.
     *
     * @return the upgrade size.
     */
    public abstract int getUpgradeSize();

    /**
     * Scans through the whole storage and updates it.
     */
    public abstract void updateItemStorage();

    /**
     * Update the blockState of the rack. Switch between connected, single, full and empty texture.
     */
    protected abstract void updateBlockState();

    /**
     * Get the other double chest or null.
     *
     * @return the tileEntity of the other half or null.
     */
    public abstract AbstractTileEntityRack getOtherChest();
}
