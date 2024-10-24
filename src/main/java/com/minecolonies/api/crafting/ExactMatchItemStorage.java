package com.minecolonies.api.crafting;

import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Used to exact match stacks when storing them.
 */
public class ExactMatchItemStorage extends ItemStorage
{
    /**
     * Creates an instance of the storage.
     *
     * @param stack the stack.
     */
    public ExactMatchItemStorage(@NotNull final ItemStack stack)
    {
        super(stack);
    }

    @Override
    public boolean equals(final Object comparisonObject)
    {
        if (this == comparisonObject)
        {
            return true;
        }
        if (comparisonObject instanceof final ExactMatchItemStorage that)
        {
            final Matcher.Builder builder = new Matcher.Builder(getItemStack().getItem());
            if (!(this.shouldIgnoreDamageValue || that.shouldIgnoreDamageValue))
            {
                builder.compareDamage(getItemStack().getDamageValue());
            }
            if (!(this.shouldIgnoreNBTValue || that.shouldIgnoreNBTValue))
            {
                builder.compareNBT(ItemNBTMatcher.EXACT_MATCH, getItemStack().getTag());
            }

            return ItemStackUtils.compareItemStack(builder.build(), this.getItemStack());

        }
        return false;
    }
}