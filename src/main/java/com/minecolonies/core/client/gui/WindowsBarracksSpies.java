package com.minecolonies.core.client.gui;

import com.ldtteam.blockui.controls.*;
import com.ldtteam.blockui.views.BOWindow;
import com.minecolonies.api.colony.buildings.views.IBuildingView;
import com.minecolonies.api.inventory.IInventory;
import com.minecolonies.api.inventory.InventoryRack;
import com.minecolonies.core.tileentities.TileEntityRack;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.inventory.InventoryUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.core.Network;
import com.minecolonies.core.network.messages.server.colony.HireSpiesMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

import static com.minecolonies.api.util.constant.TranslationConstants.DESCRIPTION_BARRACKS_HIRE_SPIES;

/**
 * UI for hiring spies on the barracks
 */
public class WindowsBarracksSpies extends BOWindow implements ButtonHandler
{
    /**
     * The xml file for this gui
     */
    private static final String SPIES_GUI_XML = ":gui/windowbarracksspies.xml";

    /**
     * The cancel button id
     */
    private static final String BUTTON_CANCEL = "cancel";

    /**
     * The hire spies button id
     */
    private static final String BUTTON_HIRE = "hireSpies";

    /**
     * The spies button icon id
     */
    private static final String SPIES_BUTTON_ICON = "hireSpiesIcon";

    /**
     * The gold amount label id
     */
    private static final String GOLD_COST_LABEL = "amount";

    /**
     * Text element id
     */
    private static final String TEXT_ID = "text";

    private static final int GOLD_COST = 5;

    /**
     * The client side colony data
     */
    private final IBuildingView buildingView;

    public WindowsBarracksSpies(final IBuildingView buildingView, final BlockPos buildingPos)
    {
        super(new ResourceLocation(Constants.MOD_ID + SPIES_GUI_XML));
        this.buildingView = buildingView;

        findPaneOfTypeByID(SPIES_BUTTON_ICON, ItemIcon.class).setItem(Items.GOLD_INGOT.getDefaultInstance());
        findPaneOfTypeByID(GOLD_COST_LABEL, Text.class).setText(Component.literal("x5"));

        int goldCount = InventoryUtils.countInPlayersInventory(Minecraft.getInstance().player, Items.GOLD_INGOT);
        if (buildingView.getColony().getWorld().getBlockEntity(buildingPos) instanceof IInventory buildingInventory)
        {
            final Matcher goldMatcher = new Matcher.Builder(Items.GOLD_INGOT).build();
            goldCount += buildingInventory.countMatches(goldMatcher);
        }

        if (!buildingView.getColony().isRaiding() || goldCount < GOLD_COST || buildingView.getColony().areSpiesEnabled())
        {
            findPaneOfTypeByID(BUTTON_HIRE, ButtonImage.class).disable();
        }
        findPaneOfTypeByID(TEXT_ID, Text.class).setText(Component.translatable(DESCRIPTION_BARRACKS_HIRE_SPIES));
    }

    @Override
    public void onButtonClicked(final Button button)
    {
        switch (button.getID())
        {
            case BUTTON_CANCEL:
            {
                this.close();
                break;
            }
            case BUTTON_HIRE:
            {
                findPaneOfTypeByID(BUTTON_HIRE, ButtonImage.class).disable();
                Network.getNetwork().sendToServer(new HireSpiesMessage(buildingView.getColony()));
                this.close();
                break;
            }
        }
    }
}
