package com.minecolonies.core.tileentities;

import com.google.common.collect.ImmutableList;
import com.ldtteam.domumornamentum.client.model.data.MaterialTextureData;
import com.ldtteam.domumornamentum.client.model.properties.ModProperties;
import com.ldtteam.domumornamentum.entity.block.IMateriallyTexturedBlockEntity;
import com.minecolonies.api.blocks.AbstractBlockMinecoloniesRack;
import com.minecolonies.api.blocks.ModBlocks;
import com.minecolonies.api.blocks.types.RackType;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.inventory.api.CombinedItemHandler;
import com.minecolonies.api.inventory.container.ContainerRack;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.api.tileentities.AbstractTileEntityRack;
import com.minecolonies.api.tileentities.MinecoloniesTileEntities;
import com.minecolonies.api.util.BlockPosUtil;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;

import static com.minecolonies.api.util.constant.Constants.*;
import static com.minecolonies.api.util.constant.NbtTagConstants.*;
import static com.minecolonies.api.util.constant.TranslationConstants.RACK;

/**
 * Tile entity for the warehouse shelves.
 */
public class TileEntityRack extends AbstractTileEntityRack implements IMateriallyTexturedBlockEntity
{
    /**
     * All Racks current version id
     */
    private static final byte VERSION = 2;

    /**
     * The racks version
     */
    private byte version = 0;

    /**
     * Size multiplier of the inventory. 0 = default value. 1 = 1*9 additional slots, and so on.
     */
    private int size = 0;

    /**
     * Last optional we created.
     */
    private LazyOptional<IItemHandler> lastOptional;

    /**
     * Static texture mappings
     */
    private static final List<ResourceLocation> textureMapping = ImmutableList.<ResourceLocation>builder()
        .add(new ResourceLocation("block/bricks"))
        .add(new ResourceLocation("block/sand"))
        .add(new ResourceLocation("block/orange_wool"))
        .add(new ResourceLocation("block/dirt"))
        .add(new ResourceLocation("block/obsidian"))
        .add(new ResourceLocation("block/polished_andesite"))
        .add(new ResourceLocation("block/andesite"))
        .add(new ResourceLocation("block/blue_wool")).build();

    private static final List<ResourceLocation> secondarytextureMapping = ImmutableList.<ResourceLocation>builder()
                                                                            .add(new ResourceLocation("block/oak_log"))
                                                                            .add(new ResourceLocation("block/spruce_log"))
                                                                            .add(new ResourceLocation("block/birch_log"))
                                                                            .add(new ResourceLocation("block/jungle_log"))
                                                                            .add(new ResourceLocation("block/acacia_log"))
                                                                            .add(new ResourceLocation("block/dark_oak_log"))
                                                                            .add(new ResourceLocation("block/mangrove_log"))
                                                                            .add(new ResourceLocation("block/crimson_stem"))
                                                                            .build();

    /**
     * Cached resmap.
     */
    private MaterialTextureData textureDataCache = new MaterialTextureData();

    /**
     * If we did a double check after startup.
     */
    private boolean checkedAfterStartup = false;

    /**
     * Create a new rack.
     * @param type the specific block entity type.
     * @param pos the position.
     * @param state its state.
     */
    public TileEntityRack(final BlockEntityType<? extends TileEntityRack> type, final BlockPos pos, final BlockState state)
    {
        super(type, pos, state);
    }

    /**
     * Create a rack with a specific inventory size.
     * @param type the specific block entity type.
     * @param pos the position.
     * @param state its state.
     * @param size the ack size.
     */
    public TileEntityRack(final BlockEntityType<? extends TileEntityRack> type, final BlockPos pos, final BlockState state, final int size)
    {
        super(type, pos, state, size);
        this.size = ((size - DEFAULT_SIZE) / SLOT_PER_LINE);
    }

    /**
     * Create a new default rack.
     * @param pos the position.
     * @param state its state.
     */
    public TileEntityRack(final BlockPos pos, final BlockState state)
    {
        super(MinecoloniesTileEntities.RACK.get(), pos, state);
    }

    @Override
    protected void updateBlockState()
    {

    }

    @Override
    public void upgradeRackSize()
    {
        ++size;
        final RackInventory inventory = new RackInventory(DEFAULT_SIZE + size * SLOT_PER_LINE);
        convertToNewInventory(inventory);
        final BlockState state = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, state, state, 0x03);
        invalidateCap();
    }

    @Override
    public void updateItemStorage()
    {
        if (level != null && !level.isClientSide)
        {
            final boolean beforeEmpty = isEmpty();
            if (getBlockState().getBlock() == ModBlocks.blockRack)
            {
                boolean afterEmpty = isEmpty();
                @Nullable final BlockEntity potentialNeighbor = getOtherChest();
                if (potentialNeighbor instanceof TileEntityRack && !((TileEntityRack) potentialNeighbor).isEmpty())
                {
                    afterEmpty = false;
                }

                if ((beforeEmpty && !afterEmpty) || (!beforeEmpty && afterEmpty))
                {
                    level.setBlockAndUpdate(getBlockPos(),
                      getBlockState().setValue(AbstractBlockMinecoloniesRack.VARIANT,
                        getBlockState().getValue(AbstractBlockMinecoloniesRack.VARIANT).getInvBasedVariant(afterEmpty)));


                    if (potentialNeighbor != null)
                    {
                        level.setBlockAndUpdate(potentialNeighbor.getBlockPos(),
                          potentialNeighbor.getBlockState()
                            .setValue(AbstractBlockMinecoloniesRack.VARIANT,
                              potentialNeighbor.getBlockState().getValue(AbstractBlockMinecoloniesRack.VARIANT).getInvBasedVariant(afterEmpty)));
                    }
                }
            }
            setChanged();
        }
    }

    @Override
    public AbstractTileEntityRack getOtherChest()
    {
        if (getBlockState().getBlock() != ModBlocks.blockRack)
        {
            return null;
        }

        final RackType type = getBlockState().getValue(AbstractBlockMinecoloniesRack.VARIANT);
        if (!type.isDoubleVariant())
        {
            return null;
        }

        final BlockEntity tileEntity = level.getBlockEntity(worldPosition.relative(getBlockState().getValue(AbstractBlockMinecoloniesRack.FACING)));
        if (tileEntity instanceof TileEntityRack && !(tileEntity instanceof AbstractTileEntityColonyBuilding))
        {
            return (AbstractTileEntityRack) tileEntity;
        }

        return null;
    }

    @Override
    public ItemStackHandler createInventory(final int slots)
    {
        return new RackInventory(slots);
    }

    @Override
    public void load(final CompoundTag compound)
    {
        super.load(compound);
        if (compound.contains(TAG_SIZE))
        {
            size = compound.getInt(TAG_SIZE);
            setInventory(createInventory(DEFAULT_SIZE + size * SLOT_PER_LINE));
        }

        final ListTag inventoryTagList = compound.getList(TAG_INVENTORY, TAG_COMPOUND);
        restoreItems(inventoryTagList);

        if (compound.contains(TAG_POS))
        {
            this.buildingPos = BlockPosUtil.read(compound, TAG_POS);
        }
        version = compound.getByte(TAG_VERSION);

        invalidateCap();

        if (level != null && level.isClientSide)
        {
            refreshTextureCache();
        }
    }

    @Override
    public void saveAdditional(final CompoundTag compound)
    {
        super.saveAdditional(compound);
        compound.putInt(TAG_SIZE, size);
        final ListTag inventoryTagList = backupItems();
        compound.put(TAG_INVENTORY, inventoryTagList);
        BlockPosUtil.write(compound, TAG_POS, buildingPos);
        compound.putByte(TAG_VERSION, version);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @NotNull
    @Override
    public CompoundTag getUpdateTag()
    {
        return this.saveWithId();
    }

    @Override
    public void onDataPacket(final Connection net, final ClientboundBlockEntityDataPacket packet)
    {
        this.load(packet.getTag());
    }

    @Override
    public void handleUpdateTag(final CompoundTag tag)
    {
        this.load(tag);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> capability, final Direction dir)
    {
        if (version != VERSION)
        {
            version = VERSION;
        }

        if (!remove && capability == ForgeCapabilities.ITEM_HANDLER)
        {
            if (lastOptional != null && lastOptional.isPresent())
            {
                return lastOptional.cast();
            }

            if (getBlockState().getBlock() != ModBlocks.blockRack)
            {
                lastOptional = LazyOptional.of(() ->
                {
                    if (this.isRemoved())
                    {
                        return new RackInventory(0);
                    }

                    return new CombinedItemHandler(RACK, getItemHandler());
                });
                return lastOptional.cast();
            }

            final RackType type = getBlockState().getValue(AbstractBlockMinecoloniesRack.VARIANT);
            if (!type.isDoubleVariant())
            {
                lastOptional = LazyOptional.of(() ->
                {
                    if (this.isRemoved())
                    {
                        return new RackInventory(0);
                    }

                    return new CombinedItemHandler(RACK, getItemHandler());
                });
                return lastOptional.cast();
            }
            else
            {
                lastOptional = LazyOptional.of(() ->
                {
                    if (this.isRemoved())
                    {
                        return new RackInventory(0);
                    }

                    final AbstractTileEntityRack other = getOtherChest();
                    if (other == null)
                    {
                        return new CombinedItemHandler(RACK, getItemHandler());
                    }

                    if (type != RackType.NO_RENDER)
                    {
                        return new CombinedItemHandler(RACK, getItemHandler(), other.getItemHandler());
                    }
                    else
                    {
                        return new CombinedItemHandler(RACK, other.getItemHandler(), getItemHandler());
                    }
                });

                return lastOptional.cast();
            }
        }
        return super.getCapability(capability, dir);
    }

    @Override
    public int getUpgradeSize()
    {
        return size;
    }

    @Override
    public void setChanged()
    {
        if (level != null)
        {
            WorldUtil.markChunkDirty(level, worldPosition);
            super.setChanged();
        }
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(final int id, @NotNull final Inventory inv, @NotNull final Player player)
    {
        refreshTextureCache();
        return new ContainerRack(id, inv, getBlockPos(), getOtherChest() == null ? BlockPos.ZERO : getOtherChest().getBlockPos());
    }

    @NotNull
    @Override
    public Component getDisplayName()
    {
        return Component.literal("Rack");
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        invalidateCap();
    }

    /**
     * Invalidates the cap
     */
    private void invalidateCap()
    {
        if (lastOptional != null && lastOptional.isPresent())
        {
            lastOptional.invalidate();
        }

        lastOptional = null;
    }

    @Override
    public void updateTextureDataWith(final MaterialTextureData materialTextureData)
    {
        // noop
    }

    /**
     * Refresh the texture mapping.
     */
    private void refreshTextureCache()
    {
        final Map<ResourceLocation, Block> resMap = new HashMap<>();
        final int displayPerSlots = this.getItemHandler().getSlots() / 4;
        int index = 0;
        boolean update = false;
        boolean alreadyAddedItem = false;

        final Map<ItemStack, Integer> mapCopy = new HashMap<>(getAllItems());
        if (this.getOtherChest() instanceof TileEntityRack neighborRack)
        {
            for (final Map.Entry<ItemStack, Integer> entry : neighborRack.getAllItems().entrySet())
            {
                int value = entry.getValue() + mapCopy.getOrDefault(entry.getKey(), 0);
                mapCopy.put(entry.getKey(), value);
            }
        }
        final List<Map.Entry<ItemStack, Integer>> list = mapCopy.entrySet().stream().sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue())).toList();

        final Queue<Block> extraBlockQueue = new ArrayDeque<>();
        final Queue<Block> itemQueue = new ArrayDeque<>();
        for (final Map.Entry<ItemStack, Integer> entry : list)
        {
            // Need more solid checks!
            if (index < textureMapping.size())
            {
                Block block = Blocks.BARREL;
                boolean isBlockItem = false;
                if (entry.getKey().getItem() instanceof BlockItem blockItem)
                {
                    block = blockItem.getBlock();
                    isBlockItem = true;
                }

                int displayRows = (int) Math.ceil((Math.max(1.0, (double) entry.getValue() / entry.getKey().getMaxStackSize())) / displayPerSlots);
                if (displayRows > 1)
                {
                    for (int i = 0; i < displayRows - 1; i++)
                    {
                        if (isBlockItem)
                        {
                            extraBlockQueue.add(block);
                        }
                        else
                        {
                            itemQueue.add(block);
                        }
                    }
                }

                if (!isBlockItem)
                {
                    if (alreadyAddedItem)
                    {
                        itemQueue.add(block);
                        continue;
                    }
                    else
                    {
                        alreadyAddedItem = true;
                    }
                }

                if (entry.getValue() < 16 && !extraBlockQueue.isEmpty())
                {
                    block = extraBlockQueue.poll();
                }

                final ResourceLocation secondaryResLoc = secondarytextureMapping.get(index);
                if (!block.defaultBlockState().isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
                {
                    resMap.put(secondaryResLoc, block);
                    block = Blocks.BARREL;
                }
                else
                {
                    resMap.put(secondaryResLoc, Blocks.AIR);
                }

                final ResourceLocation resLoc = textureMapping.get(index);
                resMap.put(resLoc, block);

                if (this.textureDataCache == null
                      || !this.textureDataCache.getTexturedComponents().getOrDefault(resLoc, Blocks.BEDROCK).equals(resMap.get(resLoc))
                      || !this.textureDataCache.getTexturedComponents().getOrDefault(secondaryResLoc, Blocks.BEDROCK).equals(resMap.get(secondaryResLoc)))
                {
                    update = true;
                }
                index++;
            }
            else
            {
                break;
            }
        }

        extraBlockQueue.addAll(itemQueue);

        for (int i = index; i < textureMapping.size(); i++)
        {
            Block block = Blocks.AIR;
            if (!extraBlockQueue.isEmpty())
            {
                block = extraBlockQueue.poll();
            }

            final ResourceLocation secondaryResLoc = secondarytextureMapping.get(i);
            if (block != Blocks.AIR && !block.defaultBlockState().isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
            {
                resMap.put(secondaryResLoc, block);
                block = Blocks.BARREL;
            }
            else
            {
                resMap.put(secondaryResLoc, Blocks.AIR);
            }

            final ResourceLocation resLoc = textureMapping.get(i);
            resMap.put(resLoc, block);

            if (this.textureDataCache == null
                  || !this.textureDataCache.getTexturedComponents().getOrDefault(resLoc, Blocks.BEDROCK).equals(resMap.get(resLoc))
                  || !this.textureDataCache.getTexturedComponents().getOrDefault(secondaryResLoc, Blocks.BEDROCK).equals(resMap.get(secondaryResLoc)))
            {
                update = true;
            }
        }

        if (update)
        {
            this.textureDataCache = new MaterialTextureData(resMap);
            this.requestModelDataUpdate();
            if (level != null)
            {
                level.sendBlockUpdated(getBlockPos(), Blocks.AIR.defaultBlockState(), getBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    @NotNull
    @Override
    public ModelData getModelData()
    {
        if (!checkedAfterStartup && level != null)
        {
            checkedAfterStartup = true;
            refreshTextureCache();
        }

        return ModelData.builder()
                 .with(ModProperties.MATERIAL_TEXTURE_PROPERTY, textureDataCache)
                 .build();
    }

    @Override
    public @NotNull MaterialTextureData getTextureData()
    {
        return textureDataCache;
    }
}
