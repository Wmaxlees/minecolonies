package com.minecolonies.api.inventory.events;

import net.minecraft.world.item.ItemStack;

public abstract class AbstractInventoryEvent
{
    public final ItemStack stack;
    
    public static enum UpdateType
    {
        UNKNOWN,
        ADD,
        REMOVE,
        CLEAR,
    }
    public final UpdateType type;

    public AbstractInventoryEvent(final ItemStack stack, final UpdateType type)
    {
        this.stack = stack;
        this.type = type;
    }
}
