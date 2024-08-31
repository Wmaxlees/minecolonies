package com.minecolonies.core.colony.jobs;

import com.minecolonies.api.client.render.modeltype.ModModelTypes;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.core.entity.ai.workers.crafting.EntityAIWorkMachinist;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class JobMachinist extends AbstractJobCrafter<EntityAIWorkMachinist, JobMachinist> {
    /**
     * Create a machinist job.
     *
     * @param entity the toolmaker.
     */
    public JobMachinist(final ICitizenData entity) {
        super(entity);
    }

    /**
     * Get the RenderBipedCitizen.Model to use when the Citizen performs this job role.
     *
     * @return Model of the citizen.
     */
    @NotNull
    @Override
    public ResourceLocation getModel() {
        return ModModelTypes.BLACKSMITH_ID;
    }

    /**
     * Generate your AI class to register.
     *
     * @return your personal AI instance.
     */
    @NotNull
    @Override
    public EntityAIWorkMachinist generateAI() {
        return new EntityAIWorkMachinist(this);
    }
}
