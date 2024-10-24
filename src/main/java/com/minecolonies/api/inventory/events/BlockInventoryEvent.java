package com.minecolonies.api.inventory.events;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class BlockInventoryEvent extends AbstractInventoryEvent
{
    public final BlockPos pos;

    public BlockInventoryEvent(final ItemStack stack, final UpdateType type, final BlockPos pos)
    {
        super(stack, type);
        this.pos = pos;
    }
}
