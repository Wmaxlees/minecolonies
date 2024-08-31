package com.minecolonies.core.entity.ai.workers.crafting;

import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.util.WorldUtil;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingMachinist;
import com.minecolonies.core.colony.jobs.JobMachinist;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;

public class EntityAIWorkMachinist extends AbstractEntityAICrafting<JobMachinist, BuildingMachinist> {
    public EntityAIWorkMachinist(JobMachinist job) {
        super(job);
        worker.setCanPickUpLoot(true);
    }

    @Override
    public Class<BuildingMachinist> getExpectedBuildingClass() {
        return BuildingMachinist.class;
    }

    @Override
    public IAIState decide() {
        if (walkToBuilding()) {
            return START_WORKING;
        }

        final IAIState craftState = getNextCraftingState();
        if (craftState != START_WORKING && !WorldUtil.isPastTime(world, 6000)) {
            return craftState;
        }

        if (wantInventoryDumped()) {
            // Wait to dump before continuing.
            return getState();
        }

        return IDLE;
    }
}
