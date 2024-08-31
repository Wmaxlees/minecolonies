package com.minecolonies.core.colony.buildings.workerbuildings;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.jobs.registry.JobEntry;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.util.constant.NbtTagConstants;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.AbstractCraftingBuildingModule;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public class BuildingMachinist extends AbstractBuilding {
    private static final String MACHINIST = "machinist";

    protected BlockPos inputLocation = BlockPos.ZERO;
    protected BlockPos outputLocation = BlockPos.ZERO;

    /**
     * The constructor of the building.
     *
     * @param c the colony
     * @param l the position
     */
    public BuildingMachinist(@NotNull final IColony c, final BlockPos l) {
        super(c, l);
    }

    @NotNull
    @Override
    public String getSchematicName() {
        return MACHINIST;
    }

    @Override
    public int getMaxBuildingLevel() {
        return 1;
    }

    @Override
    public void deserializeNBT(final CompoundTag compound) {
        super.deserializeNBT(compound);

        inputLocation =
                NbtUtils.readBlockPos(compound.getCompound(NbtTagConstants.TAG_INPUT));
        outputLocation =
                NbtUtils.readBlockPos(compound.getCompound(NbtTagConstants.TAG_OUTPUT));
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag nbt = super.serializeNBT();
        nbt.put(NbtTagConstants.TAG_INPUT, NbtUtils.writeBlockPos(inputLocation));
        nbt.put(NbtTagConstants.TAG_OUTPUT, NbtUtils.writeBlockPos(outputLocation));
        return nbt;
    }

    @Override
    public void serializeToView(@NotNull final FriendlyByteBuf buf, final boolean fullSync) {
        super.serializeToView(buf, fullSync);

        buf.writeBlockPos(inputLocation);
        buf.writeBlockPos(outputLocation);
    }

    public void setInputLocation(final BlockPos pos) {
        inputLocation = pos;
    }

    public void setOutputLocation(final BlockPos pos) {
        outputLocation = pos;
    }

    public BlockPos getInputLocation() {
        return inputLocation;
    }

    public BlockPos getOutputLocation() {
        return outputLocation;
    }

    public static class CraftingModule extends AbstractCraftingBuildingModule.PlayerDefined
    {
        /**
         * Create a new module.
         *
         * @param jobEntry the entry of the job.
         */
        public CraftingModule(final JobEntry jobEntry)
        {
            super(jobEntry);
        }

        @Override
        public boolean addRecipe(IToken<?> token)
        {
            return true;
        }
    }
}
