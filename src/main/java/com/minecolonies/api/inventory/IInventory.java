package com.minecolonies.api.inventory;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Optional;

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

public interface IInventory
{
    /**
     * Whether there are any items in the target storageblock
     *
     * @return Whether the storageblock is empty
     */
    boolean isEmpty();

    /**
     * Whether the storage block has 0 completely free slots.
     *
     * @return True if there are no free slots, false otherwise.
     */
    boolean isFull();

    /**
     * Return whether the inventory contains a matching item stack
     *
     * @param stack        The item type to compare
     * @param count        The amount that must be present
     * @param ignoreDamage Whether the items should have matching damage values
     * @return Whether the inventory contains the match
     */
    boolean hasMatch(@NotNull final Matcher matcher);

    /**
     * Return whether the inventory contains any items matching the predicate
     *
     * @param predicate The predicate to check against
     * @return Whether the storageblock has any matches
     */
    boolean hasMatch(final Predicate<ItemStack> predicate);

    /**
     * Get any matching item stacks within the inventory.
     *
     * @param predicate The predicate to test against
     * @return The list of matching item stacks
     */
    List<ItemStack> findMatches(@NotNull final Predicate<ItemStack> predicate);

        /**
     * Filters the items in the inventory and returns the list of matching items.
     *
     * @param block     Block to filter
     * @return List of item stacks
     */
    @NotNull
    List<ItemStack> findMatches(@NotNull final Block block);

    List<ItemStack> findMatches(@NotNull final Matcher matcher);

    List<ItemStack> findMatches(@NotNull final List<Matcher> matchers);

    /**
     * Finds the first ItemStack that matches the given predicate and returns it. Return
     * null if it doesn't exist.
     * 
     * @param predicate The predicate to test against
     * @return The matching stack or else null
     */
    ItemStack findFirstMatch(final Predicate<ItemStack> predicate);

    /**
     * Finds the first ItemStack that matches the given stack and parameters
     * and returns it. Return null if not match is found.
     * 
     * @param stack The stack to match against
     * @param count The stack size to match
     * @param params The parameters to determine how the match works
     * @return The matching stack or else null
     */
    ItemStack findFirstMatch(@NotNull final Matcher matcher);

    /**
     * Gets the amount of a particular item contained in the storageblock
     *
     * @param predicate The predicate used to select items
     */
    int countMatches(final Predicate<ItemStack> predicate);

    /**
     * Count the number of items of different types a building has.
     *
     * @param provider the building to check.
     * @param stacks   the stacks to check for.
     * @return Amount of occurrences of stacks that match the given stacks.
     */
    int countMatches(@NotNull final List<Matcher> matchers);

    /**
     * Gets the matching count for a specific
     * item stack and can ignore NBT and damage as well.
     *
     * @param stack             The stack to check against
     * @param ignoreDamageValue Whether to ignore damage
     * @param ignoreNBT         Whether to ignore nbt data
     * @return The count of matching items in the storageblock
     */
    int countMatches(@NotNull final Matcher matcher);

    /**
     * Gets all items and their count from the storage block.
     *
     * @return The items and their count
     */
    Map<ItemStack, Integer> getAllItems();

    /**
     * Removes an item stack matching the given predicate from the storage block
     * and returns it.
     *
     * @param predicate The predicate to match
     * @param count The amount to remove
     * @param simulate If true, actually remove the item.
     * @return The matching item stack, or ItemStack.EMPTY
     */
    List<ItemStack> extractStacks(Predicate<ItemStack> predicate, int count, ItemCountType countType, boolean simulate);

    /**
     * Removes an item stack matching the given predicate from the storage block
     * and returns it.
     *
     * @param itemStack The item stack to remove
     * @param count The amount to remove
     * @param simulate If true, don't actually remove the item.
     * @return The matching item stack, or ItemStack.EMPTY
     */
    List<ItemStack> extractStacks(@NotNull final Matcher matcher, int count, ItemCountType countType, final boolean simulate);

    ItemStack extractStack(Predicate<ItemStack> predicate, int count, ItemCountType countType, boolean simulate);
    ItemStack extractStack(@NotNull final Matcher matcher, int count, ItemCountType countType, final boolean simulate);

    /**
     * Inserts as much as possible of a given item stack into the
     * inventory.
     *
     * @param itemStack   ItemStack to add.
     * @param simulate If true, don't actually insert the item.
     * @return The remaining itemstack after insertion was complete
     */
    ItemStack insert(@Nullable ItemStack itemStack, boolean simulate);

    /**
     * Inserts as much as possible of a a given set of items into the
     * inventory.
     *
     * @param itemStacks   ItemStacks to add.
     * @param simulate If true, don't actually insert the items.
     * @return The remaining itemstacks after insertion was complete
     */
    List<ItemStack> insert(@Nullable List<ItemStack> itemStack, boolean simulate);

    /**
     * Remove all the content of the storageblock that should be sorted and return
     * it. The value of the map should be the number of individual items matching
     * the ExactMatchItemStorage and not the number of stacks.
     * 
     * @return The map of exact matches to counts of that item in the storage block
     *         that were removed for sorting.
     */
    Map<ExactMatchItemStorage, Integer> removeSortableItemStacks();

    /**
     * Create an NBT that we can use to backup the items in the
     * storage block.
     * 
     * @return The backup tag.
     */
    ListTag backupItems();

    /**
     * Restore the items in the storageblock from an NBT backup.
     * 
     * @param tag The backup to restore.
     */
    void restoreItems(ListTag tag);

    /**
     * Returns the items that are present in the list but missing in the inventory.
     * 
     * @param items The items to match against
     * @return The missing items.
     */
    List<Matcher> findMissing(@NotNull final List<Matcher> matchers);

    /**
     * Whether the inventory has enough free space to hold the items.
     * 
     * @param itemStacks the items to check for space
     * @param ignoreContents whether to ignore what the inventory contains and
     *                            just check whether hypothetically the inventory
     *                            could hold the item stack.
     * @return Whether there is enough space.
     */
    boolean hasSpaceFor(List<ItemStack> itemStacks, boolean ignoreContents);

    /**
     * Drop all items in the inventory at the given position.
     */
    void dropAllItems(Level level, BlockPos pos);

    /**
     * Insert an item int the inventory. If there are no free
     * slots, the item will be inserted into the first slot that
     * does not match the predicate.
     * 
     * @param itemStack
     * @param toKeep
     * @param simulate
     * @return
     */
    ItemStack forceInsert(@NotNull final ItemStack itemStack, @NotNull final Predicate<ItemStack> toKeep, final boolean simulate);

    /**
     * Remove all items from the inventory.
     */
    void clear();

    boolean reduceStackSize(@NotNull final Matcher matcher, int amount);

    boolean reduceStackSize(Predicate<ItemStack> predicate, int amount);

    ItemStack maybeExtractRandomStack(final int amount);

    boolean hasSimilar(Item item);

    float getPercentFull();

    void reloadCache();

    InventoryId getInventoryId();

    String getCacheDebugString();
}
