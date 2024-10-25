package com.minecolonies.api.inventory.events;

import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class EntityInventoryEvent extends AbstractInventoryEvent
{
    public final UUID entityId;

    public EntityInventoryEvent(final ItemStack stack, final UpdateType type, final UUID entityId)
    {
        super(stack, type);
        this.entityId = entityId;
    }
}
