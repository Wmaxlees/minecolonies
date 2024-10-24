package com.minecolonies.api.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.minecolonies.api.crafting.ExactMatchItemStorage;
import com.minecolonies.api.util.inventory.ItemHandlerUtils;
import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;

public abstract class InventoryItemHandler implements IInventory
{
    public abstract IItemHandler getItemHandler();

    /**
     * Get the position of the inventory if the inventory is part
     * of a block or null if the inventory is part of an entity.
     * 
     * @return The position of the inventory or null.
     */
    public abstract BlockPos getPos();

    protected InventoryCache cache = new InventoryCache();

    public InventoryItemHandler()
    {
        cache.addTarget(getPos());
    }

    @Override
    public boolean isEmpty()
    {
        return cache.isEmpty();
    }

    @Override
    public boolean isFull() 
    {
        return ItemHandlerUtils.isFull(getItemHandler());
    }

    @Override
    public boolean hasMatch(@NotNull final Matcher matcher)
    {
        return cache.hasMatch(matcher);
    }

    @Override
    public boolean hasMatch(Predicate<ItemStack> predicate)
    {
        return ItemHandlerUtils.hasMatch(getItemHandler(), predicate);
    }

    @Override
    public List<ItemStack> findMatches(@NotNull Predicate<ItemStack> predicate)
    {
        return ItemHandlerUtils.findMatches(getItemHandler(), predicate);
    }

    @Override
    public @NotNull List<ItemStack> findMatches(@NotNull Block block)
    {
        return ItemHandlerUtils.findMatches(getItemHandler(), block);
    }

    @Override
    public @NotNull List<ItemStack> findMatches(@NotNull Matcher matcher)
    {
        return cache.findMatches(matcher);
    }

    @Override
    public ItemStack findFirstMatch(Predicate<ItemStack> predicate)
    {
        IItemHandler handler = getItemHandler();
        int slot = ItemHandlerUtils.findFirstSlotMatching(handler, predicate);
        return handler.getStackInSlot(slot);
    }

    @Override
    public ItemStack findFirstMatch(@NotNull final Matcher matcher)
    {
        return cache.findFirstMatch(matcher);
    }

    @Override
    public int countMatches(Predicate<ItemStack> predicate)
    {
        return ItemHandlerUtils.countMatches(getItemHandler(), predicate);
    }

    @Override
    public int countMatches(@NotNull final Matcher matcher)
    {
        return cache.countMatches(matcher);
    }

    @Override
    public Map<ItemStack, Integer> getAllItems()
    {
        return cache.getAllItems();
    }

    @Override
    public List<ItemStack> extractStacks(Predicate<ItemStack> predicate, int count, ItemCountType countType, boolean simulate)
    {
        return ItemHandlerUtils.extractStacks(getItemHandler(), predicate, count, countType, simulate);
    }

    @Override
    public List<ItemStack> extractStacks(@NotNull final Matcher matcher, int count, ItemCountType countType, boolean simulate)
    {
        return ItemHandlerUtils.extractStacks(getItemHandler(), matcher, count, countType, simulate);
    }

    @Override
    public ItemStack insert(@Nullable ItemStack itemStack, boolean simulate)
    {
        if (itemStack == null || itemStack.isEmpty())
        {
            return ItemStackUtils.EMPTY;
        }

        return ItemHandlerUtils.insert(getItemHandler(), itemStack, simulate);
    }

    @Override
    public List<ItemStack> insert(@Nullable List<ItemStack> itemStacks, boolean simulate)
    {
        final List<ItemStack> result = new ArrayList<>();
        for (ItemStack itemStack : itemStacks)
        {
            final ItemStack insertResult = insert(itemStack, simulate);
            if (!insertResult.isEmpty())
            {
                result.add(insertResult);
            }
        }

        return result;
    }

    @Override
    public Map<ExactMatchItemStorage, Integer> removeSortableItemStacks()
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removeSortableItemStacks'");
    }

    @Override
    public ListTag backupItems()
    {
        return ItemHandlerUtils.backupItems(getItemHandler());
    }

    @Override
    public void restoreItems(ListTag tag)
    {
        ItemHandlerUtils.restoreItems(getItemHandler(), tag);
    }

    @Override
    public List<Matcher> findMissing(@NotNull final List<Matcher> matchers)
    {
        return ItemHandlerUtils.findMissing(getItemHandler(), matchers);
    }

    @Override
    public boolean hasSpaceFor(List<ItemStack> itemStacks, boolean ignoreContents) 
    {
        return ItemHandlerUtils.hasSpaceFor(getItemHandler(), itemStacks, ignoreContents);
    }
    
    @Override
    public void dropAllItems(Level level, BlockPos pos)
    {
        ItemHandlerUtils.dropItemHandler(getItemHandler(), level, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public ItemStack forceInsert(@NotNull final ItemStack itemStack, @NotNull final Predicate<ItemStack> toKeep, final boolean simulate)
    {
        final ItemStack remaining = insert(itemStack, simulate);

        if (remaining.isEmpty())
        {
            return ItemStackUtils.EMPTY;
        }

        return ItemHandlerUtils.forceInsertItemStack(getItemHandler(), remaining.copy(), toKeep);
    }

    @Override
    public boolean reduceStackSize(@NotNull final Matcher matcher, int amount)
    {
        return reduceStackSize(matcher::match, amount);
    }

    @Override
    public boolean reduceStackSize(Predicate<ItemStack> predicate, int amount)
    {
        return ItemHandlerUtils.reduceStackSize(getItemHandler(), predicate, amount);
    }

    @Override
    public boolean hasSimilar(@NotNull Item item)
    {
        return cache.hasSimilar(item);
    }

    @Override
    public List<ItemStack> findMatches(@NotNull List<Matcher> matchers)
    {
        return cache.findMatches(matchers);
    }

    @Override
    public int countMatches(@NotNull List<Matcher> matchers)
    {
        return cache.countMatches(matchers);
    }

    @Override
    public ItemStack extractStack(Predicate<ItemStack> predicate, int count, ItemCountType countType,
            boolean simulate)
    {
        return ItemHandlerUtils.extractStack(getItemHandler(), predicate, count, countType, simulate);
    }

    @Override
    public ItemStack extractStack(@NotNull Matcher matcher, int count, ItemCountType countType,
            boolean simulate)
    {
        return extractStack(matcher::match, count, countType, simulate);
    }

    @Override
    public void clear()
    {
        ItemHandlerUtils.clear(getItemHandler());
    }

    @Override
    public ItemStack maybeExtractRandomStack(int amount)
    {
        return ItemHandlerUtils.maybeExtractRandomStack(getItemHandler(), amount);
    }

    @Override
    public float getPercentFull()
    {
        if (getItemHandler().getSlots() == 0)
        {
            return 100.0f;
        }

        int filledSlots = 0;
        for (int i = 0; i < getItemHandler().getSlots(); i++)
        {
            if (!getItemHandler().getStackInSlot(i).isEmpty())
            {
                ++filledSlots;
            }
        }

        return (filledSlots / (float) getItemHandler().getSlots()) * 100.0f;
    }
}
