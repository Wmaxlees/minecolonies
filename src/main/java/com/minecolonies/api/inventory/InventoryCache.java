package com.minecolonies.api.inventory;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.inventory.events.InventoryEvent;
import com.minecolonies.api.inventory.events.IInventoryEventListener;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLEnvironment;

public final class InventoryCache implements IInventoryEventListener
{
    /**
     * The cache of items in the inventories.
     * 
     * This map goes from Item -> ItemStack -> Inventory Position or Entity Id -> Count.
     */
    private final Map<Item, Map<HashItemStack, Map<InventoryId, Integer>>> cache = new HashMap<>();

    private final Set<InventoryId> targets = new HashSet<>();

    private final String name;

    private final class HashItemStack
    {
        protected final ItemStack stack;

        public HashItemStack(final ItemStack stack)
        {
            this.stack = stack.copyWithCount(1);
        }

        @Override
        public int hashCode()
        {
            int hash = 19;
            hash = 17 * hash + stack.getItem().hashCode();
            hash = 17 * hash + stack.getDamageValue();

            final Tag tag = stack.getTag();
            hash = 17 * hash + (tag != null ? tag.hashCode() : 0);
            
            return hash;
        }

        @Override
        public boolean equals(final Object obj)
        {
            if (!(obj instanceof HashItemStack other))
            {
                return false;
            }

            final Matcher matcher = new Matcher.Builder(stack.getItem())
                .compareDamage(stack.getDamageValue())
                .compareNBT(ItemNBTMatcher.EXACT_MATCH, stack.getTag())
                .build();

            return matcher.match(other.stack);
        }
    }

    public InventoryCache(final String name)
    {
        MinecoloniesAPIProxy.getInstance().getInventoryEventManager().addListener(this);
        this.name = name;
    }

    public void cache(final ItemStack stack, final InventoryId id)
    {
        Log.getLogger().info(name + ": Caching " + stack + " to " + id);
        final Item item = stack.getItem();
        final int count = stack.getCount();
        final HashItemStack hashStack = new HashItemStack(stack);

        if (!cache.containsKey(item))
        {
            cache.put(item, new HashMap<>());
        }

        cache.computeIfAbsent(item, i -> new HashMap<>())
             .computeIfAbsent(hashStack, s -> new HashMap<>())
             .merge(id, count, Integer::sum);
    }

    public void decache(final ItemStack stack, final InventoryId id)
    {
        Log.getLogger().info(name + ": Decaching " + stack + " from " + id);
        final Item item = stack.getItem();
        final int count = stack.getCount();
        final HashItemStack hashStack = new HashItemStack(stack);

        if (!cache.containsKey(item))
        {
            return;
        }

        final Map<HashItemStack, Map<InventoryId, Integer>> stackMap = cache.get(item);
        if (!stackMap.containsKey(hashStack))
        {
            return;
        }

        final Map<InventoryId, Integer> posMap = stackMap.get(hashStack);
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
                stackMap.remove(hashStack);
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

        for (final HashItemStack entry : cache.get(item).keySet())
        {
            if (matcher.match(entry.stack))
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
        for (final Map.Entry<HashItemStack, Map<InventoryId, Integer>> entry : cache.get(item).entrySet())
        {
            if (matcher.match(entry.getKey().stack))
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

        for (final HashItemStack entry : cache.get(item).keySet())
        {
            if (matcher.match(entry.stack))
            {
                return entry.stack;
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
        for (final HashItemStack entry : cache.get(item).keySet())
        {
            if (matcher.match(entry.stack))
            {
                result.add(entry.stack);
            }
        }

        return result;
    }

    public Map<ItemStack, Integer> getAllItems()
    {
        final Map<ItemStack, Integer> result = new HashMap<>();
        for (final Map<HashItemStack, Map<InventoryId, Integer>> stackMap : cache.values())
        {
            for (final Map.Entry<HashItemStack, Map<InventoryId, Integer>> entry : stackMap.entrySet())
            {
                result.merge(entry.getKey().stack, entry.getValue().values().stream().mapToInt(Integer::intValue).sum(), Integer::sum);
            }
        }

        return result;
    }

    public boolean hasSimilar(@NotNull Item item)
    {
        for (final Item cachedItem : cache.keySet())
        {
            if (IColonyManager.getInstance().getCompatibilityManager().getCreativeTab(new ItemStorage(item)) == IColonyManager.getInstance().getCompatibilityManager().getCreativeTab(new ItemStorage(cachedItem)))
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

    public int countMatches(Predicate<ItemStack> predicate)
    {
        int count = 0;

        for (final Map<HashItemStack, Map<InventoryId, Integer>> stackMap : cache.values())
        {
            for (final Map.Entry<HashItemStack, Map<InventoryId, Integer>> entry : stackMap.entrySet())
            {
                if (predicate.test(entry.getKey().stack))
                {
                    count += entry.getValue().values().stream().mapToInt(Integer::intValue).sum();
                }
            }
        }

        return count;
    }

    @Override
    public void onInventoryEvent(InventoryEvent event)
    {
        final InventoryId id = event.inventoryId;

        if (!targets.contains(id))
        {
            return;
        }

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
        Log.getLogger().info(name + ": Clearing " + id);
        final List<Item> itemsToRemove = new ArrayList<>();
        for (final Map.Entry<Item, Map<HashItemStack, Map<InventoryId, Integer>>> stackMap : cache.entrySet())
        {
            final List<HashItemStack> stacksToRemove = new ArrayList<>();
            for (final Map.Entry<HashItemStack, Map<InventoryId, Integer>> posMap : stackMap.getValue().entrySet())
            {
                posMap.getValue().remove(id);
                if (posMap.getValue().isEmpty())
                {
                    stacksToRemove.add(posMap.getKey());
                }
            }
            
            for (final HashItemStack stack : stacksToRemove)
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

    public void clear()
    {
        cache.clear();
    }

    public void addTarget(final InventoryId id)
    {
        if (id == null)
        {
            return;
        }

        targets.add(id);
    }

    public String getDebugString()
    {
        String result = name + "------------------------:\n";

        for (final Map.Entry<Item, Map<HashItemStack, Map<InventoryId, Integer>>> stackMap : cache.entrySet())
        {
            result += stackMap.getKey() + ":\n";
            for (final Map.Entry<HashItemStack, Map<InventoryId, Integer>> posMap : stackMap.getValue().entrySet())
            {
                result += "  " + posMap.getKey().stack.getDisplayName().getString() + ":\n";
                for (final Map.Entry<InventoryId, Integer> entry : posMap.getValue().entrySet())
                {
                    result += "    " + entry.getKey() + ": " + entry.getValue() + "\n";
                }
            }
        }

        return result;
    }
}
