package com.minecolonies.core.network.messages.server.colony.citizen;

import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.ICitizenDataView;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.inventory.InventoryUtils;
import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;
import com.minecolonies.core.MineColonies;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Transfer some items from the player inventory to the Workers's Inventory.
 */
public class TransferItemsToCitizenRequestMessage extends AbstractColonyServerMessage
{
    /**
     * The id of the building.
     */
    private int citizenId;

    /**
     * How many item need to be transfer from the player inventory to the building chest.
     */
    private ItemStack itemStack;

    /**
     * How many item need to be transfer from the player inventory to the building chest.
     */
    private int quantity;

    /**
     * Empty constructor used when registering the
     */
    public TransferItemsToCitizenRequestMessage()
    {
        super();
    }

    /**
     * Creates a Transfer Items request
     *
     * @param citizenDataView Citizen of the request.
     * @param itemStack       to be take from the player for the building
     * @param quantity        of item needed to be transfered
     * @param colony          the colony of the network message
     */
    public TransferItemsToCitizenRequestMessage(final IColony colony, @NotNull final ICitizenDataView citizenDataView, final ItemStack itemStack, final int quantity)
    {
        super(colony);
        this.citizenId = citizenDataView.getId();
        this.itemStack = itemStack;
        this.quantity = quantity;
    }

    @Override
    public void fromBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        citizenId = buf.readInt();
        itemStack = buf.readItem();
        quantity = buf.readInt();
    }

    @Override
    public void toBytesOverride(@NotNull final FriendlyByteBuf buf)
    {
        buf.writeInt(citizenId);
        buf.writeItem(itemStack);
        buf.writeInt(quantity);
    }

    @Override
    protected void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony)
    {
        final ICitizenData citizenData = colony.getCitizenManager().getCivilian(citizenId);
        if (citizenData == null)
        {
            Log.getLogger().warn("TransferItemsRequestMessage citizenData is null");
            return;
        }

        final Optional<AbstractEntityCitizen> optionalEntityCitizen = citizenData.getEntity();
        if (!optionalEntityCitizen.isPresent())
        {
            Log.getLogger().warn("TransferItemsRequestMessage entity citizen is null");
            return;
        }

        final Player player = ctxIn.getSender();
        if (player == null)
        {
            return;
        }

        final boolean isCreative = player.isCreative();
        if (quantity <= 0 && !isCreative)
        {
            Log.getLogger().warn("TransferItemsRequestMessage quantity below 0");
            return;
        }

        // Inventory content before
        Map<ItemStack, Integer> previousContent = null;
        final int amountToTake;
        if (isCreative)
        {
            amountToTake = quantity;
        }
        else
        {
            final Matcher matcher = new Matcher.Builder(itemStack.getItem())
                .compareDamage(itemStack.getDamageValue())
                .compareNBT(ItemNBTMatcher.IMPORTANT_KEYS, itemStack.getTag())
                .build();
            amountToTake = Math.min(quantity, InventoryUtils.countInPlayersInventory(player, matcher));
            Log.getLogger().info("Attempting to fulfill request: " + itemStack + " with target amount: " + quantity + " Have count: " + amountToTake);
        }

        final List<ItemStack> itemsToPut = new ArrayList<>();
        int tempAmount = amountToTake;

        while (tempAmount > 0)
        {
            int count = Math.min(itemStack.getMaxStackSize(), tempAmount);
            ItemStack stack = itemStack.copy();
            stack.setCount(count);
            itemsToPut.add(stack);
            tempAmount -= count;
        }

        final AbstractEntityCitizen citizen = optionalEntityCitizen.get();

        if (!isCreative && MineColonies.getConfig().getServer().debugInventories.get())
        {
            previousContent = citizen.getInventory().getAllItems();
            for (final Map.Entry<ItemStack, Integer> entry : InventoryUtils.getAllItemsFromPlayer(player).entrySet())
            {
                previousContent.put(entry.getKey(), previousContent.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
        }

        tempAmount = 0;
        for (final ItemStack insertStack : itemsToPut)
        {
            Log.getLogger().info("Inserting " + insertStack.getCount() + " " + insertStack.getDisplayName().getString() + " into citizen inventory");
            final ItemStack remainingItemStack = citizen.getInventory().insert(insertStack, false);
            if (!ItemStackUtils.isEmpty(remainingItemStack))
            {
                Log.getLogger().info("Failed to insert " + remainingItemStack.getCount() + " " + remainingItemStack.getDisplayName().getString() + " into citizen inventory");
                tempAmount += (insertStack.getCount() - remainingItemStack.getCount());
                break;
            }
            tempAmount += insertStack.getCount();
        }

        if (!isCreative)
        {
            int amountToRemoveFromPlayer = tempAmount;
            while (amountToRemoveFromPlayer > 0)
            {
                Log.getLogger().info("Removing " + amountToRemoveFromPlayer + " " + itemStack.getDisplayName().getString() + " from player inventory");
                final Matcher matcher = new Matcher.Builder(itemStack.getItem())
                    .compareDamage(itemStack.getDamageValue())
                    .compareNBT(ItemNBTMatcher.IMPORTANT_KEYS, itemStack.getTag())
                    .build();
                final ItemStack itemsTaken = InventoryUtils.extractItemFromPlayerInventory(player, matcher, amountToRemoveFromPlayer, ItemCountType.USE_COUNT_AS_MAXIMUM, true);
                amountToRemoveFromPlayer -= ItemStackUtils.getSize(itemsTaken);
                Log.getLogger().info("Removed " + ItemStackUtils.getSize(itemsTaken) + " " + itemStack.getDisplayName().getString() + " from player inventory");
            }
        }

        if (!isCreative && previousContent != null && MineColonies.getConfig().getServer().debugInventories.get())
        {
            final Map<ItemStack, Integer> newContent = citizen.getInventory().getAllItems();
            for (final Map.Entry<ItemStack, Integer> entry : InventoryUtils.getAllItemsFromPlayer(player).entrySet())
            {
                newContent.put(entry.getKey(), newContent.getOrDefault(entry.getKey(), 0) + entry.getValue());
            }
            InventoryUtils.doItemSetsMatch(previousContent, newContent, true);
        }
    }
}
