package com.minecolonies.api.inventory.events;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.minecolonies.api.inventory.InventoryId;
import com.minecolonies.api.inventory.events.InventoryEvent.UpdateType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public class InventoryEventManager
{
    final List<IInventoryEventListener> listeners = new ArrayList<>();

    public void addListener(final IInventoryEventListener listener)
    {
        listeners.add(listener);
    }

    public void removeListener(final IInventoryEventListener listener)
    {
        listeners.remove(listener);
    }

    public void fireInventoryEvent(final InventoryEvent event)
    {
        if (event.stack.isEmpty())
        {
            return;
        }

        for (final IInventoryEventListener listener : listeners)
        {
            listener.onInventoryEvent(event);
        }
    }

    public void fireInventoryEvent(final ItemStack stack, final UpdateType type, final InventoryId inventoryId)
    {
        final InventoryEvent event = new InventoryEvent(stack, type, inventoryId);
        fireInventoryEvent(event);
    }

    public void fireClearEvent(final InventoryId inventoryId)
    {
        final InventoryEvent event = new InventoryEvent(ItemStack.EMPTY, UpdateType.CLEAR, inventoryId);
        fireInventoryEvent(event);
    }
}
