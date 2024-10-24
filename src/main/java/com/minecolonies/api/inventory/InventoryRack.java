package com.minecolonies.api.inventory;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.minecolonies.api.crafting.ExactMatchItemStorage;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Optional;

public class InventoryRack extends BlockEntity implements IInventory
{
    private static class InventoryProxy extends InventoryItemHandler
    {
        private ItemStackHandler inventory;
        private final BlockPos pos;

        public InventoryProxy(final BlockPos pos)
        {
            this.pos = pos;
        }

        public IItemHandler getItemHandler()
        {
            return inventory;
        }

        public ItemStackHandler getItemStackHandler()
        {
            return inventory;
        }

        @Override
        public BlockPos getPos()
        {
            return pos;
        }
    }

    private final InventoryProxy proxy = new InventoryProxy(getBlockPos());

    public InventoryRack(final BlockEntityType<?> tileEntityTypeIn, final BlockPos pos, final BlockState state)
    {
        super(tileEntityTypeIn, pos, state);
    }

    public void setInventory(@NotNull final ItemStackHandler inventory)
    {
        proxy.inventory = inventory;
    }

    public void convertToNewInventory(@NotNull final ItemStackHandler other)
    {
        for (int slot = 0; slot < proxy.inventory.getSlots(); ++slot)
        {
            other.insertItem(slot, proxy.inventory.getStackInSlot(slot), false);
        }

        proxy.inventory = other;
    }

    /**
     * Get the item handler for this rack. This is intended
     * to only be used for GUIs. Otherwise, prefer to not
     * interact directly with the item handler.
     * 
     * @return The item handler.
     */
    public ItemStackHandler getItemHandler()
    {
        return proxy.getItemStackHandler();
    }

    @Override
    public boolean isEmpty()
    {
        return proxy.isEmpty();
    }

    @Override
    public boolean isFull()
    {
        return proxy.isFull();
    }

    @Override
    public boolean hasMatch(@NotNull final Matcher matcher)
    {
        return proxy.hasMatch(matcher);
    }

    @Override
    public boolean hasMatch(Predicate<ItemStack> predicate)
    {
        return proxy.hasMatch(predicate);
    }

    @Override
    public List<ItemStack> findMatches(@NotNull Predicate<ItemStack> predicate)
    {
        return proxy.findMatches(predicate);
    }

    @Override
    public @NotNull List<ItemStack> findMatches(@NotNull Block block)
    {
        return proxy.findMatches(block);
    }

    @Override
    public @NotNull List<ItemStack> findMatches(@NotNull Matcher matcher)
    {
        return proxy.findMatches(matcher);
    }

    @Override
    public ItemStack findFirstMatch(Predicate<ItemStack> predicate)
    {
        return proxy.findFirstMatch(predicate);
    }

    @Override
    public ItemStack findFirstMatch(@NotNull final Matcher matcher) 
    {
        return proxy.findFirstMatch(matcher);
    }

    @Override
    public int countMatches(Predicate<ItemStack> predicate)
    {
        return proxy.countMatches(predicate);
    }

    @Override
    public int countMatches(@NotNull List<Matcher> matchers)
    {
        return proxy.countMatches(matchers);
    }

    @Override
    public int countMatches(@NotNull final Matcher matcher)
    {
        return proxy.countMatches(matcher);
    }

    @Override
    public Map<ItemStack, Integer> getAllItems()
    {
        return proxy.getAllItems();
    }

    @Override
    public List<ItemStack> extractStacks(Predicate<ItemStack> predicate, int count, ItemCountType countType,
            boolean simulate)
    {
        return proxy.extractStacks(predicate, count, countType, simulate);
    }

    @Override
    public List<ItemStack> extractStacks(@NotNull final Matcher matcher, int count, ItemCountType countType, boolean simulate)
    {
        return proxy.extractStacks(matcher, count, countType, simulate);
    }

    @Override
    public ItemStack insert(@Nullable ItemStack itemStack, boolean simulate)
    {
        return proxy.insert(itemStack, simulate);
    }

    @Override
    public List<ItemStack> insert(@Nullable List<ItemStack> itemStack, boolean simulate)
    {
        return proxy.insert(itemStack, simulate);
    }

    @Override
    public Map<ExactMatchItemStorage, Integer> removeSortableItemStacks()
    {
        return proxy.removeSortableItemStacks();
    }

    @Override
    public ListTag backupItems()
    {
        return proxy.backupItems();
    }

    @Override
    public void restoreItems(ListTag tag)
    {
        proxy.restoreItems(tag);
    }

    @Override
    public List<Matcher> findMissing(@NotNull List<Matcher> matchers)
    {
        return proxy.findMissing(matchers);
    }

    @Override
    public boolean hasSpaceFor(List<ItemStack> itemStacks, boolean ignoreContents)
    {
        return proxy.hasSpaceFor(itemStacks, ignoreContents);
    }

    @Override
    public void dropAllItems(Level level, BlockPos pos)
    {
        proxy.dropAllItems(level, pos);
    }

    @Override
    public ItemStack forceInsert(@NotNull ItemStack itemStack, @NotNull Predicate<ItemStack> toKeep, boolean simulate)
    {
        return proxy.forceInsert(itemStack, toKeep, simulate);
    }

    @Override
    public boolean hasSimilar(@NotNull Item item)
    {
        return proxy.hasSimilar(item);
    }

    @Override
    public List<ItemStack> findMatches(@NotNull List<Matcher> matchers)
    {
        return proxy.findMatches(matchers);
    }

    @Override
    public ItemStack extractStack(Predicate<ItemStack> predicate, int count, ItemCountType countType,
            boolean simulate)
    {
        return proxy.extractStack(predicate, count, countType, simulate);
    }

    @Override
    public ItemStack extractStack(@NotNull Matcher matcher, int count, ItemCountType countType, boolean simulate)
    {
        return proxy.extractStack(matcher, count, countType, simulate);
    }

    @Override
    public void clear()
    {
        proxy.clear();
    }

    @Override
    public boolean reduceStackSize(@NotNull Matcher matcher, int amount)
    {
        return proxy.reduceStackSize(matcher, amount);
    }

    @Override
    public boolean reduceStackSize(Predicate<ItemStack> predicate, int amount)
    {
        return proxy.reduceStackSize(predicate, amount);
    }

    @Override
    public ItemStack maybeExtractRandomStack(int amount)
    {
        return proxy.maybeExtractRandomStack(amount);
    }

    @Override
    public float getPercentFull()
    {
        return proxy.getPercentFull();
    }

}
