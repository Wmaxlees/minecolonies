package com.minecolonies.api.util.inventory;

import com.google.common.base.Functions;
import com.minecolonies.api.inventory.IInventory;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.MessageUtils;
import com.minecolonies.api.util.inventory.params.ItemCountType;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.minecolonies.api.util.constant.TranslationConstants.MESSAGE_INFO_PLAYER_INVENTORY_FULL_HOTBAR_INSERT;

/**
 * Utility methods for the inventories.
 */
public class InventoryUtils
{
    /**
     * Private constructor to hide the implicit one.
     */
    private InventoryUtils()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Method to swap the ItemStacks from the given source {@link IInventory} to the given target {@link IInventory}.
     *
     * @param source      The {@link IInventory} that works as Source.
     * @param targetStack The stack to transfer
     * @param count       the quantity.
     * @param target      The {@link IInventory} that works as Target.
     * @return True when the swap was successful, false when not.
     */
    public static boolean transfer(
        @NotNull final IInventory source,
        @NotNull final IInventory target,
        @NotNull final Matcher matcher,
        final int count, final ItemCountType countType)
    {
        return transferInternal(source, target,
                (Boolean simulate) -> source.extractStacks(matcher, count, countType, simulate), count, countType);
    }

    /**
     * Method to transfer an ItemStacks from the given source {@link IInventory} to the given target {@link IInventory}.
     *
     * @param source    The {@link IInventory} that works as Source.
     * @param predicate The predicate for the stack.
     * @param count     The amount to transfer
     * @param target    The {@link IInventory} that works as Target.
     * @return true when the swap was successful, false when not.
     */
    public static boolean transfer(
        @NotNull final IInventory source,
        @NotNull final IInventory target,
        final Predicate<ItemStack> predicate,
        final int count,
        final ItemCountType countType)
    {
        return transferInternal(source, target,
                (Boolean simulate) -> source.extractStacks(predicate, count, countType, simulate), count, countType);
    }

    public static boolean transfer(
        @NotNull final IInventory source,
        @NotNull final Container target,
        final int slot,
        final Predicate<ItemStack> predicate,
        final int count,
        ItemCountType countType
    )
    {
        IItemHandler handler = new InvWrapper(target);

        ItemStack sourceStack = source.extractStack(predicate, count, countType, true);

        if (sourceStack.isEmpty())
        {
            return true;
        }

        ItemStack result = handler.insertItem(slot, sourceStack, true);

        if (!result.isEmpty())
        {
            return false;
        }

        sourceStack = source.extractStack(predicate, count, countType, false);

        result = handler.insertItem(slot, sourceStack, false);

        boolean success = true;
        if (!result.isEmpty())
        {
            success = false;
            source.insert(result, false);
        }

        return success;
    }

    public static boolean transfer(
        @NotNull final Container source,
        @NotNull final IInventory target,
        final int slot,
        final int count,
        @NotNull final ItemCountType countType
    )
    {
        IItemHandler handler = new InvWrapper(source);

        ItemStack sourceStack = handler.extractItem(slot, count, true);

        if (sourceStack.isEmpty())
        {
            return true;
        }

        ItemStack result = target.insert(sourceStack, true);

        if (!result.isEmpty())
        {
            return false;
        }

        sourceStack = handler.extractItem(slot, count, false);

        result = target.insert(sourceStack, false);

        boolean success = true;
        if (!result.isEmpty())
        {
            success = false;
            handler.insertItem(slot, result, false);
        }

        return success;
    }

    public static boolean transferInternal(
        @NotNull final IInventory source,
        @NotNull final IInventory target,
        final Function<Boolean, List<ItemStack>> extractFromSource,
        final int count, final ItemCountType countType)
    {
        List<ItemStack> sourceStacks = extractFromSource.apply(true);

        if (sourceStacks.isEmpty())
        {
            Log.getLogger().info("No items found to transfer.");
            return true;
        }

        List<ItemStack> results = target.insert(sourceStacks, true);

        for (ItemStack result : results)
        {
            if (!result.isEmpty())
            {
                return false;
            }
        }

        sourceStacks = extractFromSource.apply(false);

        results = target.insert(sourceStacks, false);

        boolean success = true;
        for (ItemStack result : results)
        {
            if (!result.isEmpty())
            {
                success = false;
                source.insert(result, false);
            }
        }

        return success;
    }

    /**
     * Transfers food items from the source with the required saturation value, or as much as possible.
     *
     * @param source             to extract items from
     * @param target             to insert items into
     * @param requiredSaturation required saturation value
     * @param foodPredicate      food choosing predicate
     * @return true if any food was transferred
     */
    public static int transferFoodUpToSaturation(
      final IInventory source,
      final IInventory target,
      final int requiredSaturation,
      final Predicate<ItemStack> foodPredicate)
    {
        return transferFoodUpToSaturationInternal(requiredSaturation,
            () -> source.findMatches(foodPredicate),
            (matcherBuilder, count) -> source.extractStacks(matcherBuilder.compareCount(ItemCountType.MATCH_COUNT_EXACTLY, count).build(), count, ItemCountType.MATCH_COUNT_EXACTLY, false),
            itemStacks -> target.insert(itemStacks, false),
            itemStacks -> source.insert(itemStacks, false));
    }

    /**
     * Tries to put given itemstack in hotbar and select it, fails when player inventory is full, successes otherwise.
     *
     * @param itemStack   itemstack to put into player's inv
     * @param player player entity
     * @return true if item was put into player's inv, false if dropped
     */
    public static boolean putItemToHotbarAndSelectOrDrop(final ItemStack itemStack, final Player player)
    {
        final Inventory playerInv = player.getInventory();

        final int emptySlot = playerInv.getFreeSlot();
        if (emptySlot == -1) // try full inv first
        {
            player.drop(itemStack, false);
            return false;
        }
        else
        {
            final int hotbarSlot = playerInv.getSuitableHotbarSlot();
            final ItemStack curHotbarItem = playerInv.getItem(hotbarSlot);

            // check if we need to make space first
            if (!curHotbarItem.isEmpty())
            {
                playerInv.setItem(emptySlot, curHotbarItem);
            }

            playerInv.setItem(hotbarSlot, itemStack);
            playerInv.selected = hotbarSlot;
            playerInv.setChanged();
            updateHeldItemFromServer(player);
            return true;
        }
    }

    /**
     * Tries to put given itemstack in hotbar, fails when player inventory is full, successes otherwise.
     * If fails sends a message to player about dropped item.
     *
     * @param itemStack   itemstack to put into player's inv
     * @param player player entity
     * @return true if item was put into player's inv, false if dropped
     */
    public static boolean putItemToHotbarAndSelectOrDropMessage(final ItemStack itemStack, final Player player)
    {
        final boolean result = putItemToHotbarAndSelectOrDrop(itemStack, player);

        if (!result)
        {
            MessageUtils.format(itemStack.getDisplayName().copy())
              .append(MESSAGE_INFO_PLAYER_INVENTORY_FULL_HOTBAR_INSERT)
              .sendTo(player);
        }
        return result;
    }

    /**
     * If item is already in inventory then it's moved to hotbar and returned.
     * Else {@link #putItemToHotbarAndSelectOrDrop} is called with itemstack created from given factory.
     *
     * @param item             item to search for
     * @param player           player inventory to check and use
     * @param itemStackFactory factory for new item if not found
     * @param messageOnDrop    if true message player when new item was dropped
     * @return itemstack in hotbar or dropped in front of player
     */
    public static ItemStack getOrCreateItemAndPutToHotbarAndSelectOrDrop(final Item item,
        final Player player,
        final Supplier<ItemStack> itemStackFactory,
        final boolean messageOnDrop)
    {
        final Inventory playerInv = player.getInventory();

        for (int slot = 0; slot < playerInv.items.size(); slot++)
        {
            final ItemStack itemSlot = playerInv.getItem(slot);
            if (itemSlot.getItem() == item)
            {
                if (!Inventory.isHotbarSlot(slot))
                {
                    playerInv.pickSlot(slot);
                }
                else
                {
                    playerInv.selected = slot;
                }
                playerInv.setChanged();
                updateHeldItemFromServer(player);
                return itemSlot;
            }
        }

        final ItemStack newItem = itemStackFactory.get();
        if (messageOnDrop)
        {
            putItemToHotbarAndSelectOrDropMessage(newItem, player);
        }
        else
        {
            putItemToHotbarAndSelectOrDrop(newItem, player);
        }
        return newItem;
    }

    /**
     * Updates held item slot on client. Client autoupdates server once per tick.
     *
     * @param player player to sync
     */
    private static void updateHeldItemFromServer(final Player player)
    {
        if (player instanceof ServerPlayer)
        {
            ((ServerPlayer) player).server.getPlayerList().sendAllPlayerInfo((ServerPlayer) player);
        }
    }

    public static boolean doesPlayerHave(@NotNull final Player player, @NotNull final Block block)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.hasMatch(handler, block);
    }

    public static boolean doesPlayerHave(@NotNull final Player player, Predicate<ItemStack> predicate)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());

        return ItemHandlerUtils.findFirstSlotMatching(handler, predicate) != -1;
    }

    public static int countInPlayersInventory(Player player, Predicate<ItemStack> requestPredicate)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());

        return ItemHandlerUtils.countMatches(handler, requestPredicate);
    }

    public static @NotNull ItemStack extractItemFromPlayerInventory(Player player,
            Predicate<ItemStack> requestPredicate, int amount, ItemCountType countType, boolean ignoreArmorAndOffhand)
    {
        final IItemHandler handler = new InvWrapper(player.getInventory());
        final List<Integer> slots = ItemHandlerUtils.findMatchingSlots(handler, requestPredicate);
        int invSize = handler.getSlots();
        if (ignoreArmorAndOffhand)
        {
            invSize -= 5; // 4 armour slots + 1 shield slot
        }
        
        int slot = -1;
        for (final Integer possibleSlot : slots)
        {
            if (possibleSlot < invSize)
            {
                slot = possibleSlot;
                break;
            }
        }

        if (slot == -1)
        {
            return ItemStack.EMPTY;
        }

        return handler.extractItem(slot, countType.getRemaining(0, amount), false);
    }

    public static @NotNull ItemStack extractItemFromPlayerInventory(Player player, @NotNull final Matcher matcher, final int count, final ItemCountType countType, boolean ignoreArmorAndOffhand)
    {
        return extractItemFromPlayerInventory(player, matcher::match, count, countType, ignoreArmorAndOffhand);
    }

    public static int countInPlayersInventory(Player player, @NotNull Item item)
    {
        final Matcher matcher = new Matcher.Builder(item).build();
        return countInPlayersInventory(player, matcher::match);
    }

    public static boolean doesPlayerHave(@NotNull final Player player, @NotNull final Item item)
    {
        final Matcher matcher = new Matcher.Builder(item).build();
        return doesPlayerHave(player, matcher::match);
    }

    public static boolean insertInPlayerInventory(Player player, ItemStack stack)
    {
        return player.getInventory().add(stack);
    }

    /**
     * Transfers as many items from source to target as possible and returns any items
     * that were not successfully transferred.
     * 
     * @param source
     * @param target
     * @return
     */
    public static List<ItemStack> transferAllAndClear(IInventory source, IInventory target)
    {
        final Map<ItemStack, Integer> allItems = source.getAllItems();
        List<ItemStack> stacks = ItemStackUtils.convertToMaxSizeItemStacks(allItems);
        stacks = target.insert(stacks, false);
        source.clear();
        return stacks;
    }

    public static boolean transfer(final @NotNull IInventory source, final @NotNull List<IInventory> targets,
            final @NotNull Matcher matcher,
            final int count, final ItemCountType countType)
    {
        for (IInventory target : targets)
        {
            if (transfer(source, target, matcher, count, countType))
            {
                return true;
            }
            {
                return true;
            }
        }

        return false;
    }

    public static boolean transfer(final @NotNull List<IInventory> sources, final @NotNull IInventory target,
            final @NotNull Matcher matcher,
            final int count, final ItemCountType countType)
    {
        for (IInventory source : sources)
        {
            if (transfer(source, target, matcher, count, countType))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean transfer(final @NotNull List<IInventory> sources, final @NotNull IInventory target, Predicate<ItemStack> predicate,
            final int count, final ItemCountType countType)
    {
        for (IInventory source : sources)
        {
            if (transfer(source, target, predicate, count, countType))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean transfer(@NotNull final Container source, @NotNull final IInventory target, @NotNull final Matcher matcher,
        final int count, final ItemCountType countType)
    {
        IItemHandler handler = new InvWrapper(source);
        final int targetSlot = ItemHandlerUtils.findFirstSlotMatching(handler, matcher);
        if (targetSlot == -1)
        {
            return false;
        }

        ItemStack stack = handler.extractItem(targetSlot, matcher.getTargetCount(), false);

        if (stack.isEmpty())
        {
            return false;
        }

        stack = target.insert(stack, false);

        if (!stack.isEmpty())
        {
            handler.insertItem(targetSlot, stack, false);
            return false;
        }

        return true;
    }

    public static boolean isPlayerInventoryFull(Player player)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.isFull(handler);
    }

    public static int transferFoodUpToSaturation(@NotNull InventoryCitizen source, Player target, int requiredSaturation, Predicate<ItemStack> predicate)
    {
        return transferFoodUpToSaturationInternal(requiredSaturation,
            () -> source.findMatches(predicate),
            (matcherBuilder, count) -> source.extractStacks(matcherBuilder.compareCount(ItemCountType.MATCH_COUNT_EXACTLY, count).build(), count, ItemCountType.MATCH_COUNT_EXACTLY, false),
            itemStacks -> ItemHandlerUtils.insert(new InvWrapper(target.getInventory()), itemStacks, false),
            itemStacks -> source.insert(itemStacks, false));
    }

    public static int transferFoodUpToSaturationInternal(int requiredSaturation,
            Supplier<List<ItemStack>> findSourceMatches,
            BiFunction<Matcher.Builder, Integer, List<ItemStack>> extractStacks,
            Function<List<ItemStack>, List<ItemStack>> insertStacksTarget,
            Consumer<List<ItemStack>> insertStacksSource)
    {
        int foundSaturation = 0;
        int transferedItems = 0;
        List<ItemStack> foodStacks = findSourceMatches.get();
        for (final ItemStack foodStack : foodStacks) {
            final FoodProperties itemFood = foodStack.getItem().getFoodProperties(foodStack, null);
            if (itemFood == null)
            {
                continue;
            }

            int amount = (int) Math.round(Math.ceil((requiredSaturation - foundSaturation) / (float) itemFood.getNutrition()));

            List<ItemStack> extractedFood;
            final Matcher.Builder matcherBuilder = new Matcher.Builder(foodStack.getItem());
            if (amount > foodStack.getCount())
            {
                // Not enough yet
                foundSaturation += foodStack.getCount() * itemFood.getNutrition();
                extractedFood = extractStacks.apply(matcherBuilder, foodStack.getCount());
            }
            else
            {
                // Stack is sufficient
                extractedFood = extractStacks.apply(matcherBuilder, amount);
                foundSaturation = requiredSaturation;
            }

            transferedItems += extractedFood.stream().mapToInt(s -> s.getCount()).sum();
            extractedFood = insertStacksTarget.apply(extractedFood);
            // Insert anything that the target couldn't hold back into the source
            insertStacksSource.accept(extractedFood);

            if (foundSaturation >= requiredSaturation)
            {
                return transferedItems;
            }
        }

        return transferedItems;
    }

    public static boolean transferAll(@NotNull final IInventory source, @NotNull final InventoryCitizen target)
    {
        final Map<ItemStack, Integer> allItems = source.getAllItems();
        List<ItemStack> stacks = ItemStackUtils.convertToMaxSizeItemStacks(allItems);
        List<ItemStack> simResult = target.insert(stacks, true);

        if (!simResult.isEmpty())
        {
            return false;
        }

        target.insert(stacks, false);
        source.clear();
        return true;
    }

    public static void transferWithPossibleSwap(@NotNull final IInventory source, @NotNull final IInventory target,
            List<Matcher> matchers, final List<Integer> counts, final List<ItemCountType> countTypes, Predicate<ItemStack> toKeepPredicate)
    {
        if (matchers.size() != counts.size() || matchers.size() != countTypes.size())
        {
            throw new IllegalArgumentException("matchers, counts and countTypes must have the same size");
        }

        final List<ItemStack> toTransfer = new ArrayList<>();
        for (int i = 0; i < matchers.size(); ++i)
        {
            toTransfer.addAll(source.extractStacks(matchers.get(i), counts.get(i), countTypes.get(i), false));
        }

        for (final ItemStack stack : toTransfer)
        {
            final ItemStack swapped = target.forceInsert(stack, toKeepPredicate, false);

            if (!swapped.isEmpty())
            {
                continue;
            }

            source.insert(swapped, false);
        }
    }

    public static Map<ItemStack, Integer> getAllItemsFromPlayer(Player player)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.getAllItems(handler).stream().collect(Collectors.toMap(Functions.identity(), ItemStack::getCount));
    }

    public static boolean doItemSetsMatch(Map<ItemStack, Integer> setA, Map<ItemStack, Integer> setB, final boolean showDebugTrace)
    {
        for (Map.Entry<ItemStack, Integer> entry : setA.entrySet())
        {
            ItemStack stack = entry.getKey();
            int count = entry.getValue();
            if (!setB.containsKey(stack) || setB.get(stack) != count)
            {
                if (showDebugTrace)
                {
                    Log.getLogger().warn("ItemStacks do not match: " + stack + " " + count);
                }
                return false;
            }
        }

        for (Map.Entry<ItemStack, Integer> entry : setB.entrySet())
        {
            ItemStack stack = entry.getKey();
            int count = entry.getValue();
            if (!setA.containsKey(stack) || setA.get(stack) != count)
            {
                if (showDebugTrace)
                {
                    Log.getLogger().warn("ItemStacks do not match: " + stack + " " + count);
                }
                return false;
            }
        }

        return true;
    }

    public static int countInPlayersInventory(Player player, Matcher matcher)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.countMatches(handler, matcher);
    }

    public static boolean reducePlayerStackSize(Player player, Predicate<ItemStack> predicate, int amount)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.reduceStackSize(handler, predicate, amount);
    }

    public static boolean reducePlayerStackSize(Player player, Matcher matcher, int amount)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.reduceStackSize(handler, matcher, amount);
    }

    public static ItemStack findFirstMatchInPlayer(Player player, Matcher matcher)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        final int slot = ItemHandlerUtils.findFirstSlotMatching(handler, matcher);
        return slot == -1 ? ItemStack.EMPTY : handler.getStackInSlot(slot);
    }

    public static List<ItemStack> findMatchesInPlayer(Player player, Predicate<ItemStack> predicate)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        return ItemHandlerUtils.findMatches(handler, predicate);
    }

    public static ItemStack findFirstMatchInPlayer(Player player, Predicate<ItemStack> predicate)
    {
        IItemHandler handler = new InvWrapper(player.getInventory());
        final int slot = ItemHandlerUtils.findFirstSlotMatching(handler, predicate);
        return slot == -1 ? ItemStack.EMPTY : handler.getStackInSlot(slot);
    }

    // public static IInventory convertToInventory(@NotNull final Container container)
    // {
    //     IItemHandler handler = new InvWrapper(container);
    //     return new InventoryItemHandler() {
    //         public IItemHandler getItemHandler() {
    //             return handler;
    //         }
    //     };
    // }
}
