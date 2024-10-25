package com.minecolonies.api.inventory;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.inventory.events.AbstractInventoryEvent;
import com.minecolonies.api.inventory.events.BlockInventoryEvent;
import com.minecolonies.api.inventory.events.EntityInventoryEvent;
import com.minecolonies.api.inventory.events.IInventoryEventListener;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.inventory.Matcher;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryCache implements IInventoryEventListener
{
    /**
     * The cache of items in the inventories.
     * 
     * This map goes from Item -> ItemStack -> Inventory Position or Entity Id -> Count.
     */
    private final Map<Item, Map<ItemStack, Map<InventoryId, Integer>>> cache = new HashMap<>();

    private final Set<InventoryId> targets = new HashSet<>();

    public InventoryCache()
    {
        MinecoloniesAPIProxy.getInstance().getInventoryEventManager().addListener(this);
    }

    private void cache(final ItemStack stack, final InventoryId id)
    {
        final Item item = stack.getItem();

        Log.getLogger().info("Caching from " + id + ": " + stack.getDisplayName().getString());

        cache.computeIfAbsent(item, i -> new HashMap<>())
             .computeIfAbsent(stack, s -> new HashMap<>())
             .merge(id, stack.getCount(), Integer::sum);
    }

    private void decache(final ItemStack stack, final InventoryId id)
    {
        final Item item = stack.getItem();
        final int count = stack.getCount();

        Log.getLogger().info("Decaching from " + id + ": " + stack.getDisplayName().getString());

        if (!cache.containsKey(item))
        {
            return;
        }

        final Map<ItemStack, Map<InventoryId, Integer>> stackMap = cache.get(item);
        if (!stackMap.containsKey(stack))
        {
            return;
        }

        final Map<InventoryId, Integer> posMap = stackMap.get(stack);
        if (!posMap.containsKey(id))
        {
            return;
        }

        final int newCount = posMap.get(id) - count;
        if (newCount <= 0)
        {
            posMap.remove(id);
            if (posMap.isEmpty())
            {
                stackMap.remove(stack);
                if (stackMap.isEmpty())
                {
                    cache.remove(item);
                }
            }
        }
        else
        {
            posMap.put(id, newCount);
        }
    }

    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    public boolean hasMatch(final Matcher matcher)
    {
        final Item item = matcher.getTargetItem();
        if (!cache.containsKey(item))
        {
            return false;
        }

        for (final ItemStack entry : cache.get(item).keySet())
        {
            if (matcher.match(entry))
            {
                return true;
            }
        }

        return false;
    }

    public int countMatches(final Matcher matcher)
    {
        final Item item = matcher.getTargetItem();
        if (!cache.containsKey(item))
        {
            return 0;
        }

        int count = 0;
        for (final Map.Entry<ItemStack, Map<InventoryId, Integer>> entry : cache.get(item).entrySet())
        {
            if (matcher.match(entry.getKey()))
            {
                count += entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
            }
        }

        return count;
    }

    public ItemStack findFirstMatch(final Matcher matcher)
    {
        final Item item = matcher.getTargetItem();
        if (!cache.containsKey(item))
        {
            return ItemStack.EMPTY;
        }

        for (final ItemStack entry : cache.get(item).keySet())
        {
            if (matcher.match(entry))
            {
                return entry;
            }
        }

        return ItemStack.EMPTY;
    }

    public List<ItemStack> findMatches(final Matcher matcher)
    {
        final Item item = matcher.getTargetItem();
        if (!cache.containsKey(item))
        {
            return List.of();
        }

        final List<ItemStack> result = new ArrayList<>();
        for (final ItemStack entry : cache.get(item).keySet())
        {
            if (matcher.match(entry))
            {
                result.add(entry);
            }
        }

        return result;
    }

    public Map<ItemStack, Integer> getAllItems()
    {
        final Map<ItemStack, Integer> result = new HashMap<>();
        for (final Map<ItemStack, Map<InventoryId, Integer>> stackMap : cache.values())
        {
            for (final Map.Entry<ItemStack, Map<InventoryId, Integer>> entry : stackMap.entrySet())
            {
                result.merge(entry.getKey(), entry.getValue().values().stream().mapToInt(Integer::intValue).sum(), Integer::sum);
            }
        }

        return result;
    }

    public boolean hasSimilar(@NotNull Item item)
    {
        for (final Map.Entry<Item, Map<ItemStack, Map<InventoryId, Integer>>> entry : cache.entrySet())
        {
            if (IColonyManager.getInstance().getCompatibilityManager().getCreativeTab(new ItemStorage(item)) == IColonyManager.getInstance().getCompatibilityManager().getCreativeTab(new ItemStorage(entry.getKey())))
            {
                return true;
            }
        }
        
        return false;
    }

    public List<ItemStack> findMatches(@NotNull List<Matcher> matchers)
    {
        final List<ItemStack> result = new ArrayList<>();

        for (final Matcher matcher : matchers)
        {
            result.addAll(findMatches(matcher));
        }

        return result;
    }

    public int countMatches(@NotNull List<Matcher> matchers)
    {
        int count = 0;

        for (final Matcher matcher : matchers)
        {
            count += countMatches(matcher);
        }

        return count;
    }

    @Override
    public void onInventoryEvent(AbstractInventoryEvent event)
    {
        final InventoryId id;
        if (event instanceof BlockInventoryEvent blockEvent) {
            id = new InventoryId(blockEvent.pos);
        }
        else if (event instanceof EntityInventoryEvent entityEvent)
        {
            id = new InventoryId(entityEvent.entityId);
        }
        else
        {
            return;
        }

        if (!targets.contains(id))
        {
            return;
        }
        
        Log.getLogger().info("Processing " + event.type + " event for " + id + ": " + event.stack.getDisplayName().getString());
        switch (event.type)
        {
            case ADD:
                cache(event.stack, id);
                break;
            case REMOVE:
                decache(event.stack, id);
                break;
            case CLEAR:
                clear(id);
                break;
            default:
                break;
        }
    }

    public void clear(final InventoryId id)
    {
        final List<Item> itemsToRemove = new ArrayList<>();
        for (final Map.Entry<Item, Map<ItemStack, Map<InventoryId, Integer>>> stackMap : cache.entrySet())
        {
            final List<ItemStack> stacksToRemove = new ArrayList<>();
            for (final Map.Entry<ItemStack, Map<InventoryId, Integer>> posMap : stackMap.getValue().entrySet())
            {
                posMap.getValue().remove(id);
                if (posMap.getValue().isEmpty())
                {
                    stacksToRemove.add(posMap.getKey());
                }
            }
            
            for (final ItemStack stack : stacksToRemove)
            {
                stackMap.getValue().remove(stack);
            }

            if (stackMap.getValue().isEmpty())
            {
                itemsToRemove.add(stackMap.getKey());
            }
        }

        for (final Item item : itemsToRemove)
        {
            cache.remove(item);
        }
    }

    public void addTarget(final InventoryId id)
    {
        if (id == null)
        {
            return;
        }

        targets.add(id);
    }
}
