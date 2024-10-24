package com.minecolonies.api.util.inventory.params;

/**
 * Used for telling inventory utility functions during
 * extraction how to interpret the count parameter.
 */
public enum ItemCountType
{
    /**
     * Extract exactly n. No more. no less.
     */
    MATCH_COUNT_EXACTLY,

    /**
     * Extract at most n of the item.
     */
    USE_COUNT_AS_MAXIMUM,

    /**
     * Extract at least n of the item.
     */
    USE_COUNT_AS_MINIMUM,

    /**
     * Don't consider count at all, just extract
     * any matching items.
     */
    IGNORE_COUNT;

    public int getRemaining(int seen, int target)
    {
        switch (this)
        {
            case MATCH_COUNT_EXACTLY:
                return target - seen;
            case USE_COUNT_AS_MAXIMUM:
                return target - seen;
            case USE_COUNT_AS_MINIMUM:
                return Integer.MAX_VALUE;
            case IGNORE_COUNT:
                return Integer.MAX_VALUE;
            default:
                return 0;
        }
    }

    public boolean isSufficient(int seen, int target)
    {
        switch (this)
        {
            case MATCH_COUNT_EXACTLY:
                return seen == target;
            case USE_COUNT_AS_MAXIMUM:
                return seen <= target;
            case USE_COUNT_AS_MINIMUM:
                return seen >= target;
            case IGNORE_COUNT:
                return true;
            default:
                return false;
        }
    }
}
