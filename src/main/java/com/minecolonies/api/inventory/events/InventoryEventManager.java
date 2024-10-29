package com.minecolonies.api.inventory.events;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.minecolonies.api.inventory.InventoryId;
import com.minecolonies.api.inventory.events.InventoryEvent.UpdateType;
import com.minecolonies.api.util.Log;

import net.minecraft.world.item.ItemStack;

public class InventoryEventManager
{
    final Set<IInventoryEventListener> listeners = ConcurrentHashMap.newKeySet();

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
        if (event.stack.isEmpty() && event.type != UpdateType.CLEAR)
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
