package com.minecolonies.api.inventory.container;

import com.minecolonies.api.inventory.ModContainers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static com.minecolonies.api.util.constant.InventoryConstants.*;

public class ContainerCraftingPlayerDefined extends AbstractContainerMenu {
    private static final int CRAFT_SIZE = 3;
    private static final int RESULT_SIZE = 1;

    /** The crafting matrix inventory */
    public final CraftingContainer craftMatrix;

    /** The crafting result */
    public final CraftingContainer result;

    /** Position of container. */
    protected final BlockPos pos;

    /** The module id of the container. */
    protected final int moduleId;

    /**
     * Deserialize packet buffer to container instance.
     *
     * @param windowId the id of the window.
     * @param inv the player inventory.
     * @param packetBuffer network buffer
     * @return new instance
     */
    public static ContainerCraftingPlayerDefined fromFriendlyByteBuf(
            final int windowId, final Inventory inv, final FriendlyByteBuf packetBuffer) {
        final BlockPos tePos = packetBuffer.readBlockPos();
        final int moduleId = packetBuffer.readInt();
        return new ContainerCraftingPlayerDefined(windowId, inv, tePos, moduleId);
    }

    public ContainerCraftingPlayerDefined(
            final int windowId, final Inventory inv, final BlockPos pos, final int moduleId) {
        super(ModContainers.craftingPlayerDefined.get(), windowId);

        this.pos = pos;
        this.moduleId = moduleId;

        craftMatrix = new TransientCraftingContainer(this, CRAFT_SIZE, CRAFT_SIZE);
        result = new TransientCraftingContainer(this, RESULT_SIZE, RESULT_SIZE);

        this.addSlot(
                new Slot(result, 0, X_CRAFT_RESULT, Y_CRAFT_RESULT) {
                    @Override
                    public boolean mayPickup(final @NotNull Player playerIn) {
                        return true;
                    }
                });

        for (int i = 0; i < craftMatrix.getWidth(); ++i) {
            for (int j = 0; j < craftMatrix.getHeight(); ++j) {
                this.addSlot(
                        new Slot(
                                craftMatrix,
                                j + i * CRAFT_SIZE,
                                X_OFFSET_CRAFTING + j * INVENTORY_OFFSET_EACH,
                                Y_OFFSET_CRAFTING + i * INVENTORY_OFFSET_EACH) {
                            @Override
                            public int getMaxStackSize() {
                                return 1;
                            }

                            @NotNull
                            @Override
                            public ItemStack remove(final int par1) {
                                return ItemStack.EMPTY;
                            }

                            @Override
                            public boolean mayPlace(final @NotNull ItemStack par1ItemStack) {
                                return true;
                            }

                            @Override
                            public boolean mayPickup(final @NotNull Player par1PlayerEntity) {
                                return true;
                            }
                        });
            }
        }

        // Player inventory slots
        // Note: The slot numbers are within the player inventory and may be the same as the field
        // inventory.
        int i;
        for (i = 0; i < INVENTORY_ROWS; i++) {
            for (int j = 0; j < INVENTORY_COLUMNS; j++) {
                addSlot(
                        new Slot(
                                inv,
                                j + i * INVENTORY_COLUMNS + INVENTORY_COLUMNS,
                                PLAYER_INVENTORY_INITIAL_X_OFFSET + j * PLAYER_INVENTORY_OFFSET_EACH,
                                PLAYER_INVENTORY_INITIAL_Y_OFFSET_CRAFTING + i * PLAYER_INVENTORY_OFFSET_EACH));
            }
        }

        for (i = 0; i < INVENTORY_COLUMNS; i++) {
            addSlot(
                    new Slot(
                            inv,
                            i,
                            PLAYER_INVENTORY_INITIAL_X_OFFSET + i * PLAYER_INVENTORY_OFFSET_EACH,
                            PLAYER_INVENTORY_HOTBAR_OFFSET_CRAFTING));
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(final @NotNull Player playerIn, final int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(@NotNull final Player playerIn) {
        return true;
    }

    /**
     * Get for the container position.
     *
     * @return the position.
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Getter for the module id.
     *
     * @return the id.
     */
    public int getModuleId() {
        return this.moduleId;
    }
}
