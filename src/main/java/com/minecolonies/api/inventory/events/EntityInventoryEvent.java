package com.minecolonies.api.inventory.events;

import net.minecraft.world.item.ItemStack;

public class EntityInventoryEvent extends AbstractInventoryEvent
{
    public final int entityId;

    public EntityInventoryEvent(final ItemStack stack, final UpdateType type, final int entityId)
    {
        super(stack, type);
        this.entityId = entityId;
    }
}
