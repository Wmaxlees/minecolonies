package com.minecolonies.api.tileentities.storageblocks;

import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.tileentities.storageblocks.registry.StorageBlockEntry;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.core.tileentities.TileEntityRack;
import com.minecolonies.core.tileentities.storageblocks.ColonyBuildingStorageBlockInterface;
import com.minecolonies.core.tileentities.storageblocks.RackStorageBlockInterface;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;

public final class ModStorageBlocks
{
    public static final DeferredRegister<StorageBlockEntry> DEFERRED_REGISTER = DeferredRegister.create(new ResourceLocation(Constants.MOD_ID, "storageblocks"), Constants.MOD_ID);

    public static final RegistryObject<StorageBlockEntry> storageBlockRack;
    public static final RegistryObject<StorageBlockEntry> storageBlockColonyBuilding;

    static
    {
        storageBlockRack = DEFERRED_REGISTER.register("rack",
          () -> new StorageBlockEntry.Builder()
                  .setIsStorageBlock(blockEntity -> blockEntity instanceof TileEntityRack && !(blockEntity instanceof TileEntityColonyBuilding))
                  .setRegistryName(new ResourceLocation(Constants.MOD_ID, "rack"))
                  .setStorageInterface(RackStorageBlockInterface::new)
                  .build());

        storageBlockColonyBuilding = DEFERRED_REGISTER.register("colony_building",
          () -> new StorageBlockEntry.Builder()
                  .setIsStorageBlock(blockEntity -> blockEntity instanceof TileEntityColonyBuilding)
                  .setRegistryName(new ResourceLocation(Constants.MOD_ID, "colony_building"))
                  .setStorageInterface(ColonyBuildingStorageBlockInterface::new)
                  .build());
    }
    /**
     * Private constructor so this class can't be instantiated.
     */
    private ModStorageBlocks()
    {
    }

    /**
     * Tries to find a matching BlockInterface for the given BlockEntity
     *
     * @param pos The location of the block in the level
     * @param world The level the block is located in
     * @return A StorageBlockInterface for the given BlockEntity if one exists.
     */
    @Nullable
    public static AbstractStorageBlockInterface getStorageBlockInterface(BlockPos pos, Level world)
    {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity == null)
        {
            return null;
        }

        for (StorageBlockEntry entry : MinecoloniesAPIProxy.getInstance().getStorageBlockRegistry())
        {
            if (entry.matches(blockEntity))
            {
                return entry.getStorageInterface().apply(pos, world);
            }
        }

        return null;
    }
}
