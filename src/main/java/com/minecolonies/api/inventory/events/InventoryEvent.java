package com.minecolonies.api.inventory.events;

import com.minecolonies.api.inventory.InventoryId;

import net.minecraft.world.item.ItemStack;

public final class InventoryEvent
{
    public final InventoryId inventoryId;
    public final ItemStack stack;
    
    public static enum UpdateType
    {
        UNKNOWN,
        ADD,
        REMOVE,
        CLEAR,
    }
    public final UpdateType type;

    public InventoryEvent(final ItemStack stack, final UpdateType type, final InventoryId inventoryId)
    {
        this.stack = stack;
        this.type = type;
        this.inventoryId = inventoryId;
    }
}
