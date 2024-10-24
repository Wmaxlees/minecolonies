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
import com.minecolonies.api.util.inventory.Matcher;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class InventoryCache implements IInventoryEventListener
{
    /**
     * The cache of items in the inventories.
     * 
     * This map goes from Item -> ItemStack -> Inventory Position -> Count.
     */
    private final Map<Item, Map<ItemStack, Map<BlockPos, Integer>>> cache = new HashMap<>();

    private final Set<Integer> targetEntities = new HashSet<>();
    private final Set<BlockPos> targetBlocks = new HashSet<>();

    public InventoryCache()
    {
        MinecoloniesAPIProxy.getInstance().getInventoryEventManager().addListener(this);
    }

    private void cache(final ItemStack stack, final BlockPos pos)
    {
        final Item item = stack.getItem();

        cache.computeIfAbsent(item, i -> new HashMap<>())
             .computeIfAbsent(stack, s -> new HashMap<>())
             .merge(pos, stack.getCount(), Integer::sum);
    }

    private void decache(final ItemStack stack, final BlockPos pos)
    {
        final Item item = stack.getItem();
        final int count = stack.getCount();

        if (!cache.containsKey(item))
        {
            return;
        }

        final Map<ItemStack, Map<BlockPos, Integer>> stackMap = cache.get(item);
        if (!stackMap.containsKey(stack))
        {
            return;
        }

        final Map<BlockPos, Integer> posMap = stackMap.get(stack);
        if (!posMap.containsKey(pos))
        {
            return;
        }

        final int newCount = posMap.get(pos) - count;
        if (newCount <= 0)
        {
            posMap.remove(pos);
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
            posMap.put(pos, newCount);
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

        for (final Map.Entry<ItemStack, Map<BlockPos, Integer>> entry : cache.get(item).entrySet())
        {
            if (matcher.match(entry.getKey()))
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
        for (final Map.Entry<ItemStack, Map<BlockPos, Integer>> entry : cache.get(item).entrySet())
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

        for (final Map.Entry<ItemStack, Map<BlockPos, Integer>> entry : cache.get(item).entrySet())
        {
            if (matcher.match(entry.getKey()))
            {
                return entry.getKey();
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
        for (final Map.Entry<ItemStack, Map<BlockPos, Integer>> entry : cache.get(item).entrySet())
        {
            if (matcher.match(entry.getKey()))
            {
                result.add(entry.getKey());
            }
        }

        return result;
    }

    public Map<ItemStack, Integer> getAllItems()
    {
        final Map<ItemStack, Integer> result = new HashMap<>();
        for (final Map<ItemStack, Map<BlockPos, Integer>> stackMap : cache.values())
        {
            for (final Map.Entry<ItemStack, Map<BlockPos, Integer>> entry : stackMap.entrySet())
            {
                result.merge(entry.getKey(), entry.getValue().values().stream().mapToInt(Integer::intValue).sum(), Integer::sum);
            }
        }

        return result;
    }

    public boolean hasSimilar(@NotNull Item item)
    {
        for (final Map.Entry<Item, Map<ItemStack, Map<BlockPos, Integer>>> entry : cache.entrySet())
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
        if (event instanceof BlockInventoryEvent blockEvent && targetBlocks.contains(blockEvent.pos))
        {
            switch (blockEvent.type)
            {
                case ADD:
                    cache(blockEvent.stack, blockEvent.pos);
                    break;
                case REMOVE:
                    decache(blockEvent.stack, blockEvent.pos);
                    break;
                case CLEAR:
                    cache.clear();
                    break;
                default:
                    break;
            }
        }

        if (event instanceof EntityInventoryEvent entityEvent && targetEntities.contains(entityEvent.entityId))
        {
            switch (entityEvent.type) {
                case ADD:
                    cache(entityEvent.stack, null);
                    break;
                case REMOVE:
                    decache(entityEvent.stack, null);
                    break;
                case CLEAR:
                    cache.clear();
                    break;
                default:
                    break;
            }
        }
    }

    public void clear(final BlockPos pos)
    {
        final List<Item> itemsToRemove = new ArrayList<>();
        for (final Map.Entry<Item, Map<ItemStack, Map<BlockPos, Integer>>> stackMap : cache.entrySet())
        {
            final List<ItemStack> stacksToRemove = new ArrayList<>();
            for (final Map.Entry<ItemStack, Map<BlockPos, Integer>> posMap : stackMap.getValue().entrySet())
            {
                posMap.getValue().remove(pos);
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

    public void addTarget(final int entityId)
    {
        targetEntities.add(entityId);
    }

    public void addTarget(final BlockPos pos)
    {
        targetBlocks.add(pos);
    }
}
