package com.minecolonies.api.util.inventory.params;

/**
 * Matchers used in inventory utility functions whenever
 * item stacks need to be compared to determine how to match
 * NBT data between item stacks.
 */
public enum ItemNBTMatcher
{
    /**
     * Don't consider NBT when matching
     */
    IGNORE,

    /**
     * All NBT tags and values must match
     */
    EXACT_MATCH,

    /**
     * Only specific tags and their values must match.
     * These tags are specified in CHECKED_NBT_KEYS in
     * ItemStackUtils.
     */
    IMPORTANT_KEYS,
}
