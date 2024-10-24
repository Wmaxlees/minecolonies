package com.minecolonies.api.inventory.events;

import java.util.ArrayList;
import java.util.List;

import com.minecolonies.api.inventory.events.AbstractInventoryEvent.UpdateType;

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

    public void fireInventoryEvent(final AbstractInventoryEvent event)
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

    public void fireInventoryEvent(ItemStack stack, UpdateType type, BlockPos blockPos)
    {
        final AbstractInventoryEvent event = new BlockInventoryEvent(stack, type, blockPos);
        fireInventoryEvent(event);
    }

    public void fireInventoryEvent(ItemStack stack, UpdateType type, int entityId)
    {
        final AbstractInventoryEvent event = new EntityInventoryEvent(stack, type, entityId);
        fireInventoryEvent(event);
    }

    public void fireClearEvent(int entityId)
    {
        final AbstractInventoryEvent event = new EntityInventoryEvent(ItemStack.EMPTY, UpdateType.CLEAR, entityId);
        fireInventoryEvent(event);
    }

    public void fireClearEvent(BlockPos blockPos)
    {
        final AbstractInventoryEvent event = new BlockInventoryEvent(ItemStack.EMPTY, UpdateType.CLEAR, blockPos);
        fireInventoryEvent(event);
    }
}
