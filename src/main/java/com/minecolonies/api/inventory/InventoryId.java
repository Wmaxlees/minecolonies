package com.minecolonies.api.inventory;

import net.minecraft.core.BlockPos;
import java.util.UUID;

public class InventoryId
{
    final BlockPos pos;
    final UUID entityId;

    public InventoryId(final BlockPos pos)
    {
        this.pos = pos;
        this.entityId = null;
    }

    public InventoryId(final UUID entityId)
    {
        this.entityId = entityId;
        this.pos = null;
    }

    @Override
    public final int hashCode()
    {
        if (pos != null)
        {
            return pos.hashCode();
        }
        else
        {
            return entityId.hashCode();
        }
    }

    @Override
    public final String toString()
    {
        if (pos != null)
        {
            return pos.toString();
        }
        else
        {
            return entityId.toString();
        }
    }

    @Override
    public final boolean equals(final Object obj)
    {
        if (!(obj instanceof InventoryId invId))
        {
            return false;
        }

        return invId.entityId == entityId && invId.pos == pos;
    }
}
