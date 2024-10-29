package com.minecolonies.api.util.inventory;

import static com.minecolonies.api.util.constant.ColonyConstants.rand;
import static com.minecolonies.api.util.constant.NbtTagConstants.TAG_EMPTY;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;

public final class ItemHandlerUtils
{
    /**
     * NBT TAGS
     */
    private static final String TAG_ITEMS = "items";

    /**
     * Private constructor to prevent instantiating this class.
     */
    private ItemHandlerUtils() {}

    /**
     * Filters a list of items, matches the stack using {@link #compareItems(ItemStack, Item)}, in an {@link IItemHandler}. Uses the MetaData and {@link #getItemFromBlock(Block)}
     * as parameters for the Predicate.
     *
     * @param itemHandler Inventory to filter in
     * @param block       Block to filter
     * @return List of item stacks
     */
    @NotNull
    public static List<ItemStack> findMatches(@NotNull final IItemHandler itemHandler, @NotNull final Block block)
    {
        final Matcher comparator = new Matcher.Builder(ItemStackUtils.getItemFromBlock(block)).build();
        return findMatches(itemHandler, stack -> comparator.match(stack));
    }

    @NotNull
    public static List<ItemStack> findMatches(@NotNull final IItemHandler itemHandler, @NotNull final Matcher comparator)
    {
        return findMatches(itemHandler, stack -> comparator.match(stack));
    }

    /**
     * Filters a list of items, that match the given predicate, in an {@link IItemHandler}.
     *
     * @param itemHandler                 The IItemHandler to get items from.
     * @param itemStackSelectionPredicate The predicate to match the stack to.
     * @return List of item stacks that match the given predicate.
     */
    @NotNull
    public static List<ItemStack> findMatches(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> predicate)
    {
        @NotNull final ArrayList<ItemStack> filtered = new ArrayList<>();
        //Check every itemHandler slot
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            final ItemStack stack = itemHandler.getStackInSlot(slot);
            if (!ItemStackUtils.isEmpty(stack) && predicate.test(stack))
            {
                filtered.add(stack);
            }
        }
        return filtered;
    }

    /**
     * Returns the index of the first occurrence of the block in the {@link IItemHandler}.
     *
     * @param itemHandler {@link IItemHandler} to check.
     * @param block       Block to find.
     * @return Index of the first occurrence.
     */
    public static int findFirstSlotMatching(@NotNull final IItemHandler itemHandler, @NotNull final Block block)
    {
        final Matcher comparator = new Matcher.Builder(ItemStackUtils.getItemFromBlock(block)).build();
        return findFirstSlotMatching(itemHandler, stack -> comparator.match(stack));
    }

    public static int findFirstSlotMatching(@NotNull final IItemHandler itemHandler, @NotNull final Matcher comparator)
    {
        return findFirstSlotMatching(itemHandler, stack -> comparator.match(stack));
    }

    /**
     * Returns the index of the first occurrence of an ItemStack that matches the given predicate in the {@link IItemHandler}.
     *
     * @param itemHandler                 ItemHandler to check
     * @param itemStackSelectionPredicate The predicate to match.
     * @return Index of the first occurrence
     */
    public static int findFirstSlotMatching(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> predicate)
    {
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (predicate.test(itemHandler.getStackInSlot(slot)))
            {
                return slot;
            }
        }

        return -1;
        //TODO: Later harden contract to remove compare on slot := -1
        //throw new IllegalStateException("Item "+targetItem.getTranslationKey() + " not found in ItemHandler!");
    }

    /**
     * 
     * 
     * @param itemHandler
     * @param matcher
     * @param amount
     * @return The stacks that were removed
     */
    public static boolean reduceStackSize(IItemHandler itemHandler, @NotNull final Matcher matcher, final int amount)
    {
        return reduceStackSize(itemHandler, stack -> matcher.match(stack), amount);
    }

    /**
     * 
     * @param itemHandler
     * @param predicate
     * @param amount
     * @return The stacks that were removed
     */
    public static boolean reduceStackSize(IItemHandler itemHandler, Predicate<ItemStack> predicate, int amount)
    {
        int totalReduced = 0;

        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (predicate.test(itemHandler.getStackInSlot(slot)))
            {
                final ItemStack stack = itemHandler.getStackInSlot(slot);
                totalReduced += stack.getCount();

                if (totalReduced >= amount)
                {
                    break;
                }
            }
        }

        if (totalReduced < amount)
        {
            return false;
        }

        totalReduced = 0;
        List<ItemStack> removedStacks = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (predicate.test(itemHandler.getStackInSlot(slot)))
            {
                final ItemStack stack = itemHandler.extractItem(slot, amount - totalReduced, false);
                totalReduced += stack.getCount();
                removedStacks.add(stack);

                if (totalReduced >= amount)
                {
                    return true;
                }
            }
        }

        insert(itemHandler, removedStacks, false);
        return false;
    }

    public static List<Integer> findMatchingSlots(@NotNull final IItemHandler itemHandler, @NotNull final Matcher comparator)
    {
        return findMatchingSlots(itemHandler, stack -> comparator.match(stack));
    }

    /**
     * Returns the indexes of all occurrences of an ItemStack that matches the given predicate in the {@link IItemHandler}.
     *
     * @param itemHandler                 ItemHandler to check
     * @param itemStackSelectionPredicate The predicate to match.
     * @return list of Indexes of the occurrences
     */
    public static List<Integer> findMatchingSlots(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        final List<Integer> returnList = new ArrayList<>();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (itemStackSelectionPredicate.test(itemHandler.getStackInSlot(slot)))
            {
                returnList.add(slot);
            }
        }

        return returnList;
    }

    /**
     * Returns the amount of occurrences matching the given block.
     *
     * @param itemHandler {@link IItemHandler} to scan.
     * @param block       The block to count
     * @return Amount of occurrences of stacks that match the given block and ItemDamage
     */
    public static int countMatches(@NotNull final IItemHandler itemHandler, @NotNull final Block block)
    {
        final Matcher comparator = new Matcher.Builder(ItemStackUtils.getItemFromBlock(block)).build();
        return itemHandler == null ? 0 : countMatches(itemHandler, stack -> comparator.match(stack));
    }

    /**
     * Returns the amount of occurrences in the {@link IItemHandler}.
     *
     * @param itemHandler {@link IItemHandler} to scan.
     * @param targetItem  Item to count
     * @return Amount of occurrences of stacks that match the given item and ItemDamage
     */
    public static int countMatches(@NotNull final IItemHandler itemHandler, @NotNull final Matcher comparator)
    {
        return itemHandler == null ? 0 : countMatches(itemHandler, stack -> comparator.match(stack));
    }

    /**
     * Returns the amount of occurrences in the {@link IItemHandler}.
     *
     * @param itemHandler                 {@link IItemHandler} to scan.
     * @param itemStackSelectionPredicate The predicate used to select the stacks to count.
     * @return Amount of occurrences of stacks that match the given predicate.
     */
    public static int countMatches(@Nullable final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> itemStackSelectionPredicate)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("getItemCountInItemHandler got a null itemHandler"));
            return 0;
        }

        int count = 0;
        for (final ItemStack stack : findMatches(itemHandler, itemStackSelectionPredicate))
        {
            Log.getLogger().info("ItemHandlerUtils.countMatches: Found item " + stack.getDisplayName().getString());
            count += stack.getCount();
        }
        return count;
    }

    /**
     * Returns the amount of occurrences in the set of item handlers.
     * 
     * @param itemHandlers The set of item handlers
     * @param itemStackPredicate The predicate to match against
     * @return the count
     */
    public static int countMatches(@Nullable final Collection<IItemHandler> itemHandlers, @NotNull final Predicate<ItemStack> itemStackPredicate)
    {
        int count = 0;
        if (itemHandlers != null)
        {
            Set<ItemStack> itemSet = new HashSet<>();
            for (final IItemHandler handler : itemHandlers)
            {
                itemSet.addAll(findMatches(handler, itemStackPredicate));
            }

            for (final ItemStack stack : itemSet)
            {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Checks if a player has a block in the {@link IItemHandler}. Checked by {@link #getItemCountInItemHandler(IItemHandler, Block)} &gt; 0;
     *
     * @param itemHandler {@link IItemHandler} to scan
     * @param block       Block to count
     * @return True when in {@link IItemHandler}, otherwise false
     */
    public static boolean hasMatch(@NotNull final IItemHandler itemHandler, @NotNull final Block block)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("hasItemInItemHandler got a null itemHandler"));
            return false;
        }
        final Matcher matcher = new Matcher.Builder(ItemStackUtils.getItemFromBlock(block)).build();
        return hasMatch(itemHandler, stack -> matcher.match(stack));
    }

    /**
     * Checks if a player has an item in the {@link IItemHandler}. Checked by {@link #getItemCountInItemHandler(IItemHandler, Item)} &gt; 0;
     *
     * @param itemHandler {@link IItemHandler} to scan
     * @param item        Item to count
     * @return True when in {@link IItemHandler}, otherwise false
     */
    public static boolean hasMatch(@NotNull final IItemHandler itemHandler, @NotNull final Matcher matcher)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("hasItemInItemHandler got a null itemHandler"));
            return false;
        }
        return hasMatch(itemHandler, stack -> matcher.match(stack));
    }

    /**
     * Checks if a player has an item in the {@link IItemHandler}. Checked by {@link InventoryUtils#getItemCountInItemHandler(IItemHandler, Predicate)} &gt; 0;
     *
     * @param itemHandler                 {@link IItemHandler} to scan
     * @param itemStackSelectionPredicate The predicate to match the ItemStack to.
     * @return True when in {@link IItemHandler}, otherwise false
     */
    public static boolean hasMatch(@NotNull final IItemHandler itemHandler, @NotNull final Predicate<ItemStack> predicate)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("hasItemInItemHandler got a null itemHandler"));
            return false;
        }

        for (int i = 0; i < itemHandler.getSlots(); ++i)
        {
            if (predicate.test(itemHandler.getStackInSlot(i)))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns if the {@link IItemHandler} is full.
     *
     * @param itemHandler The {@link IItemHandler}.
     * @return True if the {@link IItemHandler} is full, false when not.
     */
    public static boolean isFull(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("hasItemInItemHandler got a null itemHandler"));
        }
        return itemHandler == null || findFirstOpenSlot(itemHandler) == -1;
    }

    /**
     * Returns if the {@link IItemHandler} is empty.
     *
     * @param itemHandler The {@link IItemHandler}.
     * @return True if the {@link IItemHandler} is empty, false when not.
     */
    public static boolean isEmpty(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("hasItemInItemHandler got a null itemHandler"));
            return false;
        }
        
        for (int i = 0; i < itemHandler.getSlots(); ++i)
        {
            if (!itemHandler.getStackInSlot(i).isEmpty())
            {
                return false;
            }
        }

        return true;
    }

     /**
     * Returns the first open slot in the {@link IItemHandler}.
     *
     * @param itemHandler The {@link IItemHandler} to check.
     * @return slot index or -1 if none found.
     */
    public static int findFirstOpenSlot(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            return -1;
        }

        for (int i = 0, slots = itemHandler.getSlots(); i < slots; i++)
        {
            final ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack == null || stack.isEmpty())
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * Count all open slots in inventory.
     *
     * @param itemHandler the inventory.
     * @return the amount of open slots.
     */
    public static long countOpenSlots(@Nullable final IItemHandler itemHandler)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("hasItemInItemHandler got a null itemHandler"));
            return 0;
        }
        return IntStream.range(0, itemHandler.getSlots())
                 .filter(slot -> ItemStackUtils.isEmpty(itemHandler.getStackInSlot(slot)))
                 .count();
    }

    /**
     * Force stack to handler.
     *
     * @param itemHandler              {@link IItemHandler} to add itemstack to.
     * @param itemStack                ItemStack to add.
     * @param itemStackToKeepPredicate The {@link Predicate} that determines which ItemStacks to keep in the inventory. Return false to replace.
     * @return itemStack which has been replaced, null if none has been replaced.
     */
    @Nullable
    public static ItemStack forceInsertItemStack(
      @NotNull final IItemHandler itemHandler,
      @NotNull final ItemStack itemStack,
      @NotNull final Predicate<ItemStack> itemStackToKeepPredicate)
    {
        final ItemStack standardInsertionResult = insert(itemHandler, itemStack, false);

        if (!ItemStackUtils.isEmpty(standardInsertionResult))
        {
            for (int i = 0; i < itemHandler.getSlots() && !ItemStackUtils.isEmpty(standardInsertionResult); i++)
            {
                final ItemStack localStack = itemHandler.getStackInSlot(i);
                if (ItemStackUtils.isEmpty(localStack) || !itemStackToKeepPredicate.test(localStack))
                {
                    final ItemStack removedStack = itemHandler.extractItem(i, Integer.MAX_VALUE, false);
                    final ItemStack localInsertionResult = itemHandler.insertItem(i, standardInsertionResult, false);

                    if (ItemStackUtils.isEmpty(localInsertionResult))
                    {
                        //Insertion successful. Returning the extracted stack.
                        return removedStack.copy();
                    }
                    else
                    {
                        //Insertion failed. The inserted stack was not accepted completely. Undo the extraction.
                        itemHandler.insertItem(i, removedStack, false);
                    }
                }
            }
        }
        return standardInsertionResult;
    }

    /**
     * Returns the amount of item stacks in an inventory. This equals {@link #getAllItems(IItemHandler)}<code>.size();</code>.
     *
     * @param itemHandler {@link IItemHandler} to count item stacks of.
     * @return Amount of item stacks in the {@link IItemHandler}.
     */
    public static int countStacks(@NotNull final IItemHandler itemHandler)
    {
        return getAllItems(itemHandler).size();
    }

    /**
     * Returns an {@link IItemHandler} as list of item stacks.
     *
     * @param itemHandler Inventory to convert.
     * @return List of item stacks.
     */
    @NotNull
    public static List<ItemStack> getAllItems(@NotNull final IItemHandler itemHandler)
    {
        return findMatches(itemHandler, (ItemStack stack) -> true);
    }

    /**
     * Returns the index of the first occurrence of an ItemStack that matches the given predicate in the {@link IItemHandler}. Also applies the not empty check.
     *
     * @param itemHandler                 ItemHandler to check
     * @param itemStackSelectionPredicate The list of predicates to match.
     * @return Index of the first occurrence
     */
    public static int findFirstNonEmptySlotMatching(final IItemHandler itemHandler, final List<Predicate<ItemStack>> itemStackSelectionPredicate)
    {
        for (final Predicate<ItemStack> predicate : itemStackSelectionPredicate)
        {
            for (int slot = 0; slot < itemHandler.getSlots(); slot++)
            {
                if (ItemStackUtils.NOT_EMPTY_PREDICATE.and(predicate).test(itemHandler.getStackInSlot(slot)))
                {
                    return slot;
                }
            }
        }

        return -1;
    }

    /**
     * Returns the index of the first occurrence of an ItemStack that matches the given predicate in the {@link IItemHandler}. Also applies the not empty check.
     *
     * @param itemHandler                 ItemHandler to check
     * @param predicate The predicate to match.
     * @return Index of the first occurrence
     */
    public static int findFirstNonEmptySlotMatching(final IItemHandler itemHandler, final Predicate<ItemStack> predicate)
    {
        if (itemHandler == null)
        {
            Log.getLogger().error("This is not supposed to happen, please notify the developers!", new Exception("findFirstNonEmptySlotMatching got a null itemHandler"));
            return -1;
        }

        @NotNull final Predicate<ItemStack> firstWorthySlotPredicate = ItemStackUtils.NOT_EMPTY_PREDICATE.and(predicate);

        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            if (firstWorthySlotPredicate.test(itemHandler.getStackInSlot(slot)))
            {
                return slot;
            }
        }

        return -1;
        //TODO: Later harden contract to remove compare on slot := -1
        //throw new IllegalStateException("Item "+targetItem.getTranslationKey() + " not found in ItemHandler!");
    }

    /**
     * Checks if the {@link IItemHandler} contains the following equipmentType with the given minimal Level.
     *
     * @param itemHandler  The {@link IItemHandler} to scan.
     * @param equipmentType     The equipmentType of the equipment to find.
     * @param minimalLevel The minimal level to find.
     * @param maximumLevel The maximum level to find.
     * @return True if equipment with the given EquipmentType was found in the given {@link IItemHandler}, false when not.
     */
    public static boolean containsEquipment(
      @NotNull final IItemHandler itemHandler,
      @NotNull final EquipmentTypeEntry equipmentType,
      final int minimalLevel,
      final int maximumLevel)
    {
        return hasMatch(itemHandler, stack -> ItemStackUtils.hasEquipmentLevel(stack, equipmentType, minimalLevel, maximumLevel));
    }

    /**
     * Clears an entire {@link IItemHandler}.
     *
     * @param itemHandler {@link IItemHandler} to clear.
     */
    public static void clear(@NotNull final IItemHandler itemHandler)
    {
        for (int slotIndex = 0; slotIndex < itemHandler.getSlots(); slotIndex++)
        {
            itemHandler.extractItem(slotIndex, Integer.MAX_VALUE, false);
        }
    }

    /**
     * Returns a slot number if an {@link IItemHandler} contains given equipment type.
     *
     * @param itemHandler  the {@link IItemHandler} to get the slot from.
     * @param equipmentType     the equipment type to look for.
     * @param minimalLevel The minimal level to find.
     * @param maximumLevel The maximum level to find.
     * @return slot number if found, -1 if not found.
     */
    public static int findFirstMatchingSlotOfEquipment(
      @NotNull final IItemHandler itemHandler, @NotNull final EquipmentTypeEntry equipmentType, final int minimalLevel,
      final int maximumLevel)
    {
        return findFirstSlotMatching(itemHandler, stack -> ItemStackUtils.hasEquipmentLevel(stack, equipmentType, minimalLevel, maximumLevel));
    }

    /**
     * Verifies if there is one equipment with an acceptable level in a worker's inventory.
     *
     * @param itemHandler   the worker's inventory
     * @param equipmentType      the type of equipment needed
     * @param requiredLevel the minimum equipment level
     * @param maximumLevel  the worker's hut level
     * @return true if equipment is acceptable
     */
    public static boolean hasEquipmentWithLevel(
      @NotNull final IItemHandler itemHandler,
      final EquipmentTypeEntry equipmentType,
      final int requiredLevel,
      final int maximumLevel)
    {
        return findFirstSlotMatching(itemHandler,
          stack -> (!ItemStackUtils.isEmpty(stack) && (equipmentType.checkIsEquipment(stack) && ItemStackUtils.verifyEquipmentLevel(stack,
            equipmentType.getMiningLevel(stack),
            requiredLevel, maximumLevel)))) > -1;
    }

    /**
     * Method to put a given Itemstack in a given target {@link IItemHandler}. Trying to merge existing itemStacks if possible.
     *
     * @param targetHandler The {@link IItemHandler} that works as Target.
     * @param stack         the itemStack to transfer.
     * @return the rest of the stack.
     */
    public static ItemStack insert(@NotNull final IItemHandler targetHandler, final ItemStack stack, boolean simulate)
    {
        ItemStack sourceStack = stack.copy();

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return sourceStack;
        }

        sourceStack = mergeItemStack(targetHandler, sourceStack);

        if (ItemStackUtils.isEmpty(sourceStack))
        {
            return sourceStack;
        }

        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            sourceStack = targetHandler.insertItem(i, sourceStack, simulate);
            if (ItemStackUtils.isEmpty(sourceStack))
            {
                return sourceStack;
            }
        }

        return sourceStack;
    }

    /**
     * Method to put a given Itemstack in a given target {@link IItemHandler}. Trying to merge existing itemStacks if possible.
     *
     * @param targetHandler The {@link IItemHandler} that works as Target.
     * @param stack         the itemStack to transfer.
     * @return the rest of the stack.
     */
    public static List<ItemStack> insert(@NotNull final IItemHandler targetHandler, final List<ItemStack> stacks, boolean simulate)
    {
        List<ItemStack> result = new ArrayList<>();

        for (ItemStack stack : stacks)
        {
            result.add(insert(targetHandler, stack, simulate));
        }

        return result;
    }

    /**
     * Method to merge the ItemStacks from the given source {@link IItemHandler} to the given target {@link IItemHandler}. Trying to merge itemStacks or returning stack if not
     * possible.
     *
     * @param targetHandler The {@link IItemHandler} that works as Target.
     * @param stack         the stack to add.
     * @return True when the swap was successful, false when not.
     */
    public static ItemStack mergeItemStack(@NotNull final IItemHandler targetHandler, final ItemStack stack)
    {
        if (ItemStackUtils.isEmpty(stack))
        {
            return stack;
        }

        ItemStack sourceStack = stack.copy();
        final Matcher comparator = new Matcher.Builder(stack.getItem())
            .compareDamage(stack.getDamageValue())
            .compareNBT(ItemNBTMatcher.EXACT_MATCH, stack.getTag())
            .build();
        for (int i = 0; i < targetHandler.getSlots(); i++)
        {
            if (!ItemStackUtils.isEmpty(targetHandler.getStackInSlot(i))
                    && comparator.match(targetHandler.getStackInSlot(i)))
            {
                sourceStack = targetHandler.insertItem(i, sourceStack, false);
                if (ItemStackUtils.isEmpty(sourceStack))
                {
                    return sourceStack;
                }
            }
        }
        return sourceStack;
    }

    public static ItemStack extractStack(@NotNull final IItemHandler handler, @NotNull final Matcher matcher, int count, ItemCountType countType, final boolean simulate)
    {
        return extractStack(handler, stack -> matcher.match(stack), count, countType, simulate);
    }

    public static ItemStack extractStack(@NotNull final IItemHandler handler, @NotNull final Predicate<ItemStack> predicate, int count, ItemCountType countType, final boolean simulate)
    {
        for (int i = 0; i < handler.getSlots(); i++)
        {
            final ItemStack stack = handler.getStackInSlot(i);
            if (predicate.test(stack) && countType.isSufficient(stack.getCount(), count))
            {
                final ItemStack extracted = handler.extractItem(i, countType.getRemaining(0, count), simulate);
                if (!ItemStackUtils.isEmpty(extracted))
                {
                    return extracted;
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * Force remove a stack with a certain amount from a given Itemhandler
     *
     * @param handler the itemHandler.
     * @param input   the stack to remove.
     * @param count   the amount to remove.
     * @param matchDamage Whether to match damage when finding stacks to remove
     * @param matchNBT    Whether to match NBT when finding stacks to remove
     * @param countType   How the count should be interpreted
     * @return The actual stacks that were removed
     */
    public static List<ItemStack> extractStacks(@NotNull final IItemHandler handler, @NotNull final Matcher matcher, int count, ItemCountType countType, boolean simulate)
    {
        return extractStacks(handler,
                itemStack -> matcher.match(itemStack),
                count, countType, simulate);
    }

    public static List<ItemStack> extractStacks(@NotNull final IItemHandler handler, @NotNull final List<Matcher> matchers, int count, ItemCountType countType, boolean simulate)
    {
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < matchers.size(); i++)
        {
            final Matcher comparator = matchers.get(i);
            int seen = 0;
            for (int slot = 0; slot < handler.getSlots(); ++slot)
            {
                final ItemStack stack = handler.getStackInSlot(slot);
                if (ItemStackUtils.isEmpty(stack))
                {
                    continue;
                }

                if (!comparator.match(stack))
                {
                    continue;
                }

                final ItemStack extractResult = handler.extractItem(slot, comparator.getRemaining(seen), simulate);
                if (extractResult.isEmpty())
                {
                    continue;
                }

                seen += extractResult.getCount();
                result.add(extractResult);

                if (comparator.isSufficient(seen))
                {
                    break;
                }
            }

            if (comparator.isSufficient(seen))
            {
                break;
            }
        }

        return result;
    }

    public static List<ItemStack> extractStacks(@NotNull final List<IItemHandler> handlers, @NotNull final List<Matcher> matchers, @NotNull final List<Integer> counts, @NotNull final List<ItemCountType> countTypes, boolean simulate)
    {
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < matchers.size(); i++)
        {
            final Matcher matcher = matchers.get(i);
            int seen = 0;
            for (final IItemHandler handler : handlers) {
                for (int slot = 0; slot < handler.getSlots(); ++slot)
                {
                    final ItemStack stack = handler.getStackInSlot(slot);
                    if (ItemStackUtils.isEmpty(stack))
                    {
                        continue;
                    }

                    if (!matcher.match(stack))
                    {
                        continue;
                    }

                    final ItemStack extractResult = handler.extractItem(slot, matcher.getRemaining(seen), simulate);
                    if (extractResult.isEmpty())
                    {
                        continue;
                    }

                    seen += extractResult.getCount();
                    result.add(extractResult);

                    if (countTypes.get(i).isSufficient(seen, counts.get(i)))
                    {
                        break;
                    }
                }

                if (countTypes.get(i).isSufficient(seen, counts.get(i)))
                {
                    break;
                }
            }

            if (countTypes.get(i).isSufficient(seen, counts.get(i)))
            {
                break;
            }
        }

        return result;
    }

    public static List<ItemStack> extractStacks(final IItemHandler handler, final Predicate<ItemStack> predicate,
            int count, ItemCountType countType, boolean simulate)
    {
        int seen = 0;
        List<Integer> matchingSlots = new ArrayList<>();
        for (int i = 0; i < handler.getSlots() && countType.getRemaining(seen, count) > 0; ++i)
        {
            ItemStack workingStack = handler.getStackInSlot(i);
            if (!predicate.test(workingStack))
            {
                continue;
            }

            workingStack = handler.extractItem(i, countType.getRemaining(seen, count), true);

            seen += workingStack.getCount();
            matchingSlots.add(i);
        }

        if (!countType.isSufficient(seen, count))
        {
            return List.of();
        }

        seen = 0;
        List<ItemStack> result = new ArrayList<>();
        for (int slot : matchingSlots)
        {
            ItemStack removed = handler.extractItem(slot, countType.getRemaining(seen, count), simulate);
            if (removed.isEmpty())
            {
                continue;
            }

            seen += removed.getCount();
            result.add(removed);

            if (countType.getRemaining(seen, count) <= 0)
            {
                break;
            }
        }

        return result;
    }

    /**
     * Check if a certain item is in the handler but without the provider being full. Return as soon as an empty slot and a matching slot has been found. Returns the last matching
     * slot it found.
     *
     * @param handler                     the handler to check.
     * @param itemStackSelectionPredicate the selection predicate..
     * @param amount                      stack size to be considered.
     * @return the slot or -1.
     */
    public static int findNonFullSlotMatching(
      final IItemHandler handler,
      @NotNull final Predicate<ItemStack> itemStackSelectionPredicate,
      final int requiredSpace)
    {
        boolean foundEmptySlot = false;
        boolean foundItem = false;
        int itemSlot = -1;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStackUtils.isEmpty(stack))
            {
                foundEmptySlot = true;
            }
            else if (itemStackSelectionPredicate.test(stack))
            {
                if (ItemStackUtils.getSize(stack) + requiredSpace <= stack.getMaxStackSize())
                {
                    foundEmptySlot = true;
                }
                foundItem = true;
                itemSlot = slot;
            }

            if (foundItem && foundEmptySlot)
            {
                return itemSlot;
            }
        }

        return -1;
    }

     /**
     * Check if a similar item is in the handler but without the provider being full. Return as soon as an empty slot and a matching slot has been found. Returns the last matching
     * slot it found.
     *
     * @param handler the handler to check.
     * @param inStack the ItemStack
     * @return true if fitting.
     */
    public static boolean findNonFullSlotMatching(
      final IItemHandler handler,
      final ItemStack inStack)
    {
        if (handler == null)
        {
            return false;
        }

        final Matcher comparator = new Matcher.Builder(inStack.getItem())
            .compareDamage(inStack.getDamageValue())
            .compareNBT(ItemNBTMatcher.EXACT_MATCH, inStack.getTag())
            .build();

        boolean foundEmptySlot = false;
        boolean foundItem = false;
        for (int slot = 0; slot < handler.getSlots(); slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStackUtils.isEmpty(stack))
            {
                foundEmptySlot = true;
            }
            else if (comparator.match(stack))
            {
                if (ItemStackUtils.getSize(stack) + ItemStackUtils.getSize(inStack) <= stack.getMaxStackSize())
                {
                    foundEmptySlot = true;
                }
                foundItem = true;
            }

            if (foundItem && foundEmptySlot)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Drop an actual itemHandler in the world.
     *
     * @param handler the handler.
     * @param world   the world.
     * @param x       the x pos.
     * @param y       the y pos.
     * @param z       the z pos.
     */
    public static void dropItemHandler(final IItemHandler handler, final Level world, final int x, final int y, final int z)
    {
        for (int i = 0; i < handler.getSlots(); ++i)
        {
            final ItemStack itemstack = handler.extractItem(i, Integer.MAX_VALUE, false);

            if (itemstack != null && !itemstack.isEmpty())
            {
                ItemStackUtils.spawnItemStack(world, x, y, z, itemstack);
            }
        }
    }

    /**
     * Checks if all stacks given in the list are in the itemhandler given
     *
     * @param stacks  The stacks that should be in the itemhandler
     * @param handler The itemhandler to check in
     * @return True when all stacks are in the handler, false when not
     */
    public static boolean matchAllItems(@NotNull final IItemHandler handler, @NotNull final List<Matcher> comparators)
    {
        return matchAllItems(ImmutableList.of(handler), comparators);
    }

    /**
     * Checks if all stacks given in the list are in at least one of the given the itemhandlers
     *
     * @param stacks   The stacks that should be in the itemhandlers
     * @param handlers The itemhandlers to check in
     * @return True when all stacks are in at least one of the handlers, false when not
     */
    public static boolean matchAllItems(@NotNull final Collection<IItemHandler> handlers, @NotNull final List<Matcher> comparators)
    {
        if (handlers.isEmpty())
        {
            return false;
        }

        if (comparators.isEmpty())
        {
            return true;
        }

        List<Matcher.State> states = comparators.stream()
            .map(Matcher::getBaseState)
            .collect(Collectors.toList());

        for (final IItemHandler handler : handlers)
        {
            boolean allSufficient = true;
            for (int comparatorIndex = 0; comparatorIndex < comparators.size(); ++comparatorIndex)
            {
                if (states.get(comparatorIndex).isSufficient())
                {
                    continue;
                }

                allSufficient = false;
                for (int slot = 0; slot < handler.getSlots(); ++slot)
                {
                    var newState = comparators.get(comparatorIndex).match(handler.getStackInSlot(slot), states.get(comparatorIndex));
                    states.set(comparatorIndex, newState.getA());
                }
            }

            if (allSufficient)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * This method calculates the amount of items in itemstacks are contained within a list.
     *
     * @param stacks The stacks to count.
     * @return A map with a entry for each unique unified itemstack and its count in the list.
     */
    public static Map<ItemStack, Integer> getMergedCountedStacksFromList(@NotNull final List<ItemStack> stacks)
    {
        final Map<ItemStack, Integer> requiredCountForStacks = Maps.newHashMap();
        stacks.forEach(targetStack -> {
            final Matcher comparator = new Matcher.Builder(targetStack.getItem())
                .compareDamage(targetStack.getDamageValue())
                .compareNBT(ItemNBTMatcher.EXACT_MATCH, targetStack.getTag())
                .build();

            final Optional<ItemStack>
              alreadyContained = requiredCountForStacks.keySet().stream()
                    .filter(itemStack -> comparator.match(itemStack))
                    .findFirst();

            if (alreadyContained.isPresent())
            {
                requiredCountForStacks.put(alreadyContained.get(), requiredCountForStacks.get(alreadyContained.get()) + targetStack.getCount());
            }
            else
            {
                requiredCountForStacks.put(targetStack, targetStack.getCount());
            }
        });

        return requiredCountForStacks;
    }

    /**
     * Searches a given itemhandler for the stacks given and returns the list that is contained in the itemhandler
     *
     * @param stacks  The stacks to search for
     * @param handler The handler to search in
     * @return The sublist of the stacks list contained in the itemhandler.
     */
    public static List<ItemStack> findMatches(@NotNull final IItemHandler handler, @NotNull final List<Matcher> comparators)
    {
        final List<ItemStack> result = Lists.newArrayList();

        for (final ItemStack itemStack : getAllItems(handler))
        {
            for (final Matcher comparator : comparators)
            {
                if (comparator.match(itemStack, comparator.getBaseState()).getB())
                {
                    result.add(itemStack);
                    break;
                }
            }
        }

        ItemStackUtils.mergeCounts(result);
        return result;
    }

    /**
     * Attempts a swap with the given itemstacks, from the source to the target inventory. Itemstacks in the target that match the given toKeepInTarget predicate will not be
     * swapped out, if swapping is needed
     *
     * @param targetInventory   The target inventory.
     * @param sourceInventories The source inventory.
     * @param toSwap            The list of stacks to swap.
     * @param toKeepInTarget    The predicate that determines what not to swap in the target.
     */
    public static void transferItemStacksWithPossibleSwap(
      @NotNull final List<IItemHandler> sourceInventories,
      @NotNull final IItemHandler targetInventory,
      @NotNull final List<Matcher> matchers,
      @NotNull final List<Integer> counts,
      @NotNull final List<ItemCountType> countTypes,
      @NotNull final Predicate<ItemStack> toKeepInTarget)
    {
        if (targetInventory.getSlots() < matchers.size())
        {
            return;
        }

        final Predicate<ItemStack> wantToKeep = toKeepInTarget
                .or(stack -> matchers.stream().anyMatch(matcher -> matcher.match(stack, matcher.getBaseState()).getB()));

        List<ItemStack> itemsToInsert = extractStacks(sourceInventories, matchers, counts, countTypes, false);

        for (ItemStack stack : itemsToInsert)
        {
            ItemStack forcingResult = forceInsertItemStack(targetInventory, stack, wantToKeep);

            if (forcingResult != null && !forcingResult.isEmpty())
            {
                for (IItemHandler sourceInventory : sourceInventories)
                {
                    forcingResult = insert(sourceInventory, forcingResult, false);

                    if (forcingResult.isEmpty())
                    {
                        break;
                    }
                }
            }
        }
    }

    // /**
    //  * Sums up all items in the given handlers
    //  *
    //  * @param handlerList inventory handlers
    //  * @return Map of IdentityItemstorage
    //  */
    // public static Map<ItemStorage, ItemStorage> getAllItems(Set<IItemHandler> handlerList)
    // {
    //     final Map<ItemStorage, ItemStorage> storageMap = new HashMap<>();
    //     for (final IItemHandler handler : handlerList)
    //     {
    //         for (int i = 0; i < handler.getSlots(); i++)
    //         {
    //             final ItemStack containedStack = handler.getStackInSlot(i);
    //             if (!ItemStackUtils.isEmpty(containedStack))
    //             {
    //                 final ItemStorage storage = new ItemStorage(containedStack.copy(), false, false);

    //                 if (storageMap.containsKey(storage))
    //                 {
    //                     final ItemStorage existing = storageMap.get(storage);
    //                     existing.setAmount(existing.getAmount() + storage.getAmount());
    //                 }
    //                 else
    //                 {
    //                     storageMap.put(storage, storage);
    //                 }
    //             }
    //         }
    //     }

    //     return storageMap;
    // }

    public static ListTag backupItems(IItemHandler itemHandler)
    {
        final ListTag inventoryTagList = new ListTag();
        for (int slot = 0; slot < itemHandler.getSlots(); slot++)
        {
            @NotNull final CompoundTag inventoryCompound = new CompoundTag();
            final ItemStack stack = itemHandler.getStackInSlot(slot);
            if (stack.isEmpty())
            {
                inventoryCompound.putBoolean(TAG_EMPTY, true);
            }
            else
            {
                stack.save(inventoryCompound);
            }
            inventoryTagList.add(inventoryCompound);
        }
        return inventoryTagList;
    }

    public static void restoreItems(IItemHandler itemHandler, ListTag items)
    {
        clear(itemHandler);
        for (int i = 0; i < items.size(); i++)
        {
            final CompoundTag inventoryCompound = items.getCompound(i);
            if (!inventoryCompound.contains(TAG_EMPTY))
            {
                final ItemStack stack = ItemStack.of(inventoryCompound);
                itemHandler.insertItem(i, stack, false);
            }
        }
    }

    public static List<Matcher> findMissing(@NotNull final IItemHandler itemHandler, @NotNull final List<Matcher> matchers)
    {
        List<Matcher> missing = new ArrayList<>();
        for (final Matcher matcher : matchers)
        {
            int found = findMatches(itemHandler, matcher).stream()
                    .map(itemStack -> itemStack.getCount()).mapToInt(i -> i).sum();
            if (!matcher.isSufficient(found))
            {
                missing.add(matcher.getUpdated(found));
            }
        }

        return missing;
    }

    public static boolean hasSpaceFor(IItemHandler itemHandler, List<ItemStack> itemStacks, boolean ignoreContents)
    {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'hasSpaceFor'");
    }

    public static int countMatches(IItemHandler itemHandler, @NotNull List<Matcher> matchers)
    {
        int count = 0;
        for (Matcher matcher : matchers)
        {
            count += countMatches(itemHandler, stack -> matcher.match(stack));
        }

        return count;
    }

    public static boolean hasSimilar(IItemHandler itemHandler, @NotNull Item item)
    {
        for (int i = 0; i < itemHandler.getSlots(); i++)
        {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (stack.getItem() == item)
            {
                return true;
            }

            if (IColonyManager.getInstance().getCompatibilityManager().getCreativeTab(new ItemStorage(item)) == IColonyManager.getInstance().getCompatibilityManager().getCreativeTab(new ItemStorage(stack)))
            {
                return true;
            }
        }
        
        return false;
    }

    public static ItemStack maybeExtractRandomStack(IItemHandler itemHandler, int amount)
    {
        return itemHandler.extractItem(rand.nextInt(itemHandler.getSlots()), 5, false);
    }
}
