package com.minecolonies.core.blocks.huts;

import com.minecolonies.api.blocks.AbstractBlockHut;
import com.minecolonies.api.colony.buildings.ModBuildings;
import com.minecolonies.api.colony.buildings.registry.BuildingEntry;
import org.jetbrains.annotations.NotNull;

public class BlockHutMachinist  extends AbstractBlockHut<BlockHutMachinist> {
    @NotNull
    @Override
    public String getHutName() {
        return "blockhutmachinist";
    }

    @Override
    public BuildingEntry getBuildingEntry() {
        return ModBuildings.machinist.get();
    }
}
