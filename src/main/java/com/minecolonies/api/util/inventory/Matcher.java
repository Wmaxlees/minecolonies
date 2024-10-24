package com.minecolonies.api.util.inventory;

import static com.minecolonies.api.util.inventory.ItemStackUtils.CHECKED_NBT_KEYS;

import com.google.common.base.Objects;
import com.minecolonies.api.items.CheckedNbtKey;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public final class Matcher
{
    protected final Item targetItem;

    protected final ItemCountType countMatcher;
    protected final int targetCount;

    protected final int targetDamage;

    protected final ItemNBTMatcher nbtMatcher;
    protected final CompoundTag targetTag;

    protected Matcher(final Builder builder)
    {
        this.targetItem = builder.targetItem;
        this.countMatcher = builder.countMatcher;
        this.targetCount = builder.targetCount;
        this.targetDamage = builder.targetDamage;
        this.nbtMatcher = builder.nbtMatcher;
        this.targetTag = builder.targetTag;
    }

    public State getBaseState()
    {
        return new State(targetCount, countMatcher);
    }

    public static class Builder
    {
        private Item targetItem;

        private ItemCountType countMatcher = ItemCountType.IGNORE_COUNT;
        private int targetCount;

        private int targetDamage;

        private ItemNBTMatcher nbtMatcher = ItemNBTMatcher.IGNORE;
        private CompoundTag targetTag;

        public Builder(final Item targetItem)
        {
            this.targetItem = targetItem;
        }

        public Builder compareCount(final ItemCountType sizeMatcher, final int targetCount)
        {
            this.countMatcher = sizeMatcher;
            this.targetCount = targetCount;
            return this;
        }

        public Builder compareDamage(final int targetDamage)
        {
            this.targetDamage = targetDamage;
            return this;
        }

        public Builder compareNBT(final ItemNBTMatcher nbtMatcher, final CompoundTag targetTag)
        {
            this.nbtMatcher = nbtMatcher;
            this.targetTag = targetTag;
            return this;
        }

        public Matcher build()
        {
            return new Matcher(this);
        }
    }

    public static class State
    {
        private final int currentCount;
        private final int targetCount;
        private final ItemCountType countMatcher;

        private State(final int targetCount, ItemCountType countMatcher)
        {
            this.currentCount = 0;
            this.targetCount = targetCount;
            this.countMatcher = countMatcher;
        }
        
        public boolean isSufficient()
        {
            return countMatcher.isSufficient(currentCount, targetCount);
        }

        public int getRemaining()
        {
            return countMatcher.getRemaining(currentCount, targetCount);
        }

        private State(final int currentCount, final int targetCount, final ItemCountType countMatcher)
        {
            this.currentCount = currentCount;
            this.targetCount = targetCount;
            this.countMatcher = countMatcher;
        }
    }

    /**
     * Checks if the ItemStack matches the comparator and returns an updated state and
     * whether the stack matches the comparator
     * 
     * @param stack
     * @return
     */
    public Tuple<State, Boolean> match(final ItemStack stack, final State currentState)
    {
       if (compareItems(stack) && compareDamage(stack) && compareNBT(stack))
       {
            return new Tuple<>(new State(currentState.currentCount + stack.getCount(), currentState.targetCount, currentState.countMatcher), true);
       }

       return new Tuple<>(currentState, false);
    }

    public Matcher mergeCounts(final Matcher other)
    {
        if (!(other instanceof Matcher))
        {
            return null;
        }

        if (!canMerge(other))
        {
            return null;
        }

        return new Matcher.Builder(targetItem)
          .compareCount(countMatcher, targetCount + other.targetCount)
          .compareDamage(targetDamage)
          .compareNBT(nbtMatcher, targetTag)
          .build();
    }

    public int getRemaining(int seen)
    {
        return countMatcher.getRemaining(seen, targetCount);
    }

    public boolean isSufficient(int seen)
    {
        return countMatcher.isSufficient(seen, targetCount);
    }

    public int getTargetCount()
    {
        return targetCount;
    }

    public ItemCountType getCountType()
    {
        return countMatcher;
    }

    protected boolean compareItems(final ItemStack stack)
    {
        return stack.getItem() == targetItem;
    }

    protected boolean compareDamage(final ItemStack stack)
    {
        return targetDamage == 0 || stack.getDamageValue() == targetDamage;
    }

    protected int compareCount(final ItemStack stack)
    {
        final int count = stack.getCount();
        switch (countMatcher)
        {
            case MATCH_COUNT_EXACTLY:
            case USE_COUNT_AS_MINIMUM:
                return Math.max(0, targetCount - count);
            default:
                break;
        }

        return 0;
    }

    protected boolean compareNBT(final ItemStack stack)
    {
        final CompoundTag tag = stack.getTag();
        return compareNBT(stack.getItem(), tag);
    }

    protected boolean compareNBT(final Item item, final CompoundTag tag)
    {
        switch (nbtMatcher)
        {
            case EXACT_MATCH:
                if (!Objects.equal(tag, targetTag))
                {
                    return false;
                }
                break;
            case IMPORTANT_KEYS:
                final Set<CheckedNbtKey> checkedKeys = CHECKED_NBT_KEYS.getOrDefault(item, null);
                
                if (checkedKeys == null)
                {
                    return tag != null && targetTag != null && tag.equals(targetTag);
                }

                if ((tag == null) != (targetTag == null) && !checkedKeys.isEmpty())
                {
                    return false;
                }

                if (checkedKeys.isEmpty())
                {
                    return true;
                }

                if (tag == null && targetTag == null)
                {
                    return true;
                }

                for (final CheckedNbtKey key : checkedKeys)
                {
                    if (!key.matches(tag, targetTag))
                    {
                        return false;
                    }
                }
                break;
            default:
                break;
        }

        return true;
    }

    public Item getTargetItem()
    {
        return targetItem;
    }

    public boolean canMerge(final Matcher other)
    {
        return targetItem == other.targetItem && targetDamage == other.targetDamage && countMatcher == other.countMatcher && nbtMatcher == other.nbtMatcher && compareNBT(targetItem, other.targetTag);
    }

    public boolean match(final ItemStack stack)
    {
       if (compareItems(stack) && compareDamage(stack) && compareNBT(stack))
       {
            return compareCount(stack) == 0;
       }

       return false;
    }

    public Matcher getUpdated(final int count)
    {
        return new Matcher.Builder(targetItem)
          .compareCount(countMatcher, targetCount - count)
          .compareDamage(targetDamage)
          .compareNBT(nbtMatcher, targetTag)
          .build();
    }
}
