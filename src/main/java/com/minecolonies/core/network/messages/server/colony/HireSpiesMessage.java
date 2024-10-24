package com.minecolonies.core.network.messages.server.colony;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.util.inventory.InventoryUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.core.network.messages.server.AbstractColonyServerMessage;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.NetworkEvent;

import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingBarracks.SPIES_GOLD_COST;

/**
 * Message for hiring spies at the cost of gold.
 */
public class HireSpiesMessage extends AbstractColonyServerMessage
{
    public HireSpiesMessage()
    {
    }

    public HireSpiesMessage(final IColony colony)
    {
        super(colony);
    }

    @Override
    protected void onExecute(final NetworkEvent.Context ctxIn, final boolean isLogicalServer, final IColony colony)
    {
        final Player player = ctxIn.getSender();
        if (player == null)
        {
            return;
        }

        if (InventoryUtils.countInPlayersInventory(player, stack -> stack.getItem() == Items.GOLD_INGOT) >= SPIES_GOLD_COST)
        {
            final Matcher goldMatcher = new Matcher.Builder(Items.GOLD_INGOT).build();
            InventoryUtils.reducePlayerStackSize(player, goldMatcher, SPIES_GOLD_COST);
            colony.getRaiderManager().setSpiesEnabled(true);
            colony.markDirty();
        }
    }

    @Override
    protected void toBytesOverride(final FriendlyByteBuf buf)
    {


    }

    @Override
    protected void fromBytesOverride(final FriendlyByteBuf buf)
    {

    }
}
