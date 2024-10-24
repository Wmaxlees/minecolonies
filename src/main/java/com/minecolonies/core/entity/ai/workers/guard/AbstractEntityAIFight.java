package com.minecolonies.core.entity.ai.workers.guard;

import com.minecolonies.api.entity.ai.workers.util.GuardGear;
import com.minecolonies.api.entity.ai.workers.util.GuardGearBuilder;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.equipment.ModEquipmentTypes;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.util.inventory.InventoryUtils;
import com.minecolonies.api.util.inventory.ItemHandlerUtils;
import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;
import com.minecolonies.core.colony.buildings.AbstractBuildingGuards;
import com.minecolonies.core.colony.jobs.AbstractJobGuard;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.research.util.ResearchConstants.SHIELD_USAGE;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.GuardConstants.*;
import static com.minecolonies.api.util.constant.EquipmentLevelConstants.*;

/**
 * Class taking of the abstract guard methods for both archer and knights.
 *
 * @param <J> the generic job.
 */
public abstract class AbstractEntityAIFight<J extends AbstractJobGuard<J>, B extends AbstractBuildingGuards> extends AbstractEntityAIInteract<J, B>
{

    /**
     * Tools and Items needed by the worker.
     */
    public final List<EquipmentTypeEntry> toolsNeeded = new ArrayList<>();

    /**
     * List of items that are required by the guard based on building level and guard level.  This array holds a pointer to the building level and then pointer to GuardGear
     */
    public final List<List<GuardGear>> itemsNeeded = new ArrayList<>();

    /**
     * The current target for our guard.
     */
    protected LivingEntity target = null;

    /**
     * The value of the speed which the guard will move.
     */
    private static final double COMBAT_SPEED = 1.0;

    /**
     * The bonus speed per worker level.
     */
    public static final double SPEED_LEVEL_BONUS = 0.01;

    /**
     * Creates the abstract part of the AI. Always use this constructor!
     *
     * @param job the job to fulfill
     */
    public AbstractEntityAIFight(@NotNull final J job)
    {
        super(job);
        super.registerTargets(
          new AITarget(IDLE, START_WORKING, 1),
          new AITarget(START_WORKING, this::startWorkingAtOwnBuilding, 100),
          new AITarget(PREPARING, this::prepare, TICKS_SECOND)
        );
        worker.setCanPickUpLoot(true);

        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_LEATHER, ARMOR_LEVEL_GOLD, LEATHER_BUILDING_LEVEL_RANGE, GOLD_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_LEATHER, ARMOR_LEVEL_CHAIN, LEATHER_BUILDING_LEVEL_RANGE, CHAIN_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_LEATHER, ARMOR_LEVEL_IRON, LEATHER_BUILDING_LEVEL_RANGE, IRON_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_CHAIN, ARMOR_LEVEL_DIAMOND, LEATHER_BUILDING_LEVEL_RANGE, DIA_BUILDING_LEVEL_RANGE));
        itemsNeeded.add(GuardGearBuilder.buildGearForLevel(ARMOR_LEVEL_IRON, ARMOR_LEVEL_MAX, LEATHER_BUILDING_LEVEL_RANGE, DIA_BUILDING_LEVEL_RANGE));
    }

    /**
     * Redirects the guard to their building.
     *
     * @return The next {@link IAIState}.
     */
    protected IAIState startWorkingAtOwnBuilding()
    {
        if (walkToBuilding())
        {
            return getState();
        }
        return PREPARING;
    }

    @Override
    public IAIState afterRequestPickUp()
    {
        return PREPARING;
    }

    @Override
    public IAIState getStateAfterPickUp()
    {
        return PREPARING;
    }

    /**
     * Prepares the guard. Fills his required armor and tool lists and transfer from building chest if required.
     *
     * @return The next {@link IAIState}.
     */
    private IAIState prepare()
    {
        for (final EquipmentTypeEntry tool : toolsNeeded)
        {
            if (checkForToolOrWeapon(tool))
            {
                return getState();
            }
            worker.getInventory().equipTool(InteractionHand.MAIN_HAND,
              stack -> !ItemStackUtils.isEmpty(stack)
                         && ItemStackUtils.doesItemServeAsWeapon(stack)
                         && ItemStackUtils.hasEquipmentLevel(stack, tool, 0, building.getMaxEquipmentLevel()));
        }

        equipInventoryArmor();

        // Can only "see" the inventory and check for items if at the building
        if (worker.blockPosition().distSqr(building.getID()) > 50)
        {
            return DECIDE;
        }

        atBuildingActions();
        return DECIDE;
    }

    /**
     * Task to do when at the own building, as guards only go there on requests and on dump
     */
    protected void atBuildingActions()
    {
        for (final GuardGear item : itemsNeeded.get(building.getBuildingLevel() - 1))
        {
            if (!(building.getBuildingLevel() >= item.getMinBuildingLevelRequired() && building.getBuildingLevel() <= item.getMaxBuildingLevelRequired()))
            {
                continue;
            }
            if (item.getItemNeeded() == ModEquipmentTypes.shield.get()
                  && worker.getCitizenColonyHandler().getColony().getResearchManager().getResearchEffects().getEffectStrength(SHIELD_USAGE) <= 0)
            {
                continue;
            }

            ItemStack bestItem = ItemStack.EMPTY;
            int bestLevel = -1;

            if (item.getType().isArmor())
            {
                if (!ItemStackUtils.isEmpty(worker.getInventory().getArmorInSlot(item.getType())))
                {
                    bestLevel = item.getItemNeeded().getMiningLevel(worker.getInventory().getArmorInSlot(item.getType()));
                }
            }
            else
            {
                if (!ItemStackUtils.isEmpty(worker.getItemBySlot(item.getType())))
                {
                    bestLevel = item.getItemNeeded().getMiningLevel(worker.getItemBySlot(item.getType()));
                }
            }


            final List<ItemStack> items = building.findMatches(item);
            if (items.isEmpty())
            {
                // None found, check for equipped
                if ((item.getType().isArmor()
                        && ItemStackUtils.isEmpty(worker.getInventory().getArmorInSlot(item.getType())))
                        || (!item.getType().isArmor()
                                && ItemStackUtils.isEmpty(worker.getItemBySlot(
                                        item.getType()))))
                {
                    // create request
                    checkForToolOrWeaponAsync(item.getItemNeeded(), item.getMinArmorLevel(), item.getMaxArmorLevel());
                }
            }
            else
            {
                // Compare levels
                for (ItemStack foundItem : items)
                {
                    if (foundItem.isEmpty())
                    {
                        continue;
                    }

                    int currentLevel = item.getItemNeeded().getMiningLevel(foundItem);

                    if (currentLevel > bestLevel)
                    {
                        bestLevel = currentLevel;
                        bestItem = foundItem;
                    }
                }
            }

            // Transfer if needed
            if (!bestItem.isEmpty())
            {
                if (item.getType().isArmor())
                {
                    if (!ItemStackUtils.isEmpty(worker.getInventory().getArmorInSlot(item.getType())))
                    {
                        final ItemStack armorStack = worker.getInventory().getArmorInSlot(item.getType());
                        worker.getInventory().moveArmorToInventory(item.getType());
                        final Matcher matcher = new Matcher.Builder(armorStack.getItem())
                          .compareDamage(armorStack.getDamageValue())
                          .compareNBT(ItemNBTMatcher.EXACT_MATCH, armorStack.getTag())
                          .build();
                        InventoryUtils.transfer(worker.getInventory(), building.getInventories(), matcher, 1, ItemCountType.MATCH_COUNT_EXACTLY);
                    }
                }
                else
                {
                    if (!ItemStackUtils.isEmpty(worker.getItemBySlot(item.getType())))
                    {
                        final ItemStack weaponStack = worker.getItemBySlot(item.getType());
                        final Matcher matcher = new Matcher.Builder(weaponStack.getItem())
                            .compareDamage(weaponStack.getDamageValue())
                            .compareNBT(ItemNBTMatcher.EXACT_MATCH, weaponStack.getTag())
                            .build();
                        InventoryUtils.transfer(worker.getInventory(), building.getInventories(), matcher, 1, ItemCountType.MATCH_COUNT_EXACTLY);
                    }

                    // Used for further comparisons, set to the right inventory slot afterwards
                    final Matcher matcher = new Matcher.Builder(bestItem.getItem())
                      .compareDamage(bestItem.getDamageValue())
                      .compareNBT(ItemNBTMatcher.EXACT_MATCH, bestItem.getTag())
                      .build();
                    InventoryUtils.transfer(building.getInventories(), worker.getInventory(), matcher, 1, ItemCountType.MATCH_COUNT_EXACTLY);
                    worker.setItemSlot(item.getType(), bestItem);
                }
            }
        }

        equipInventoryArmor();
    }

    @Override
    public IAIState afterDump()
    {
        return PREPARING;
    }

    /**
     * Equips armor existing in inventory
     */
    public void equipInventoryArmor()
    {
        cleanVisibleSlots();
        final Set<EquipmentSlot> equipment = new HashSet<>();
        for (final GuardGear item : itemsNeeded.get(building.getBuildingLevel() - 1))
        {
            if (equipment.contains(item.getType()))
            {
                continue;
            }
            if (item.getType().isArmor())
            {
                if (building.getBuildingLevel() >= item.getMinBuildingLevelRequired() && building.getBuildingLevel() <= item.getMaxBuildingLevelRequired())
                {
                    final ItemStack workerOwnedItem = worker.getInventory().findFirstMatch(item);
                    if (workerOwnedItem.isEmpty())
                    {
                        continue;
                    }

                    equipment.add(item.getType());
                    final ItemStack current = worker.getInventory().getArmorInSlot(item.getType());
                    if (!current.isEmpty() && current.getItem() instanceof ArmorItem)
                    {
                        final int currentLevel = item.getItemNeeded().getMiningLevel(current);
                        final int newLevel = item.getItemNeeded().getMiningLevel(workerOwnedItem);
                        if (currentLevel > newLevel)
                        {
                            continue;
                        }
                    }
                    final Matcher matcher = new Matcher.Builder(workerOwnedItem.getItem())
                      .compareDamage(workerOwnedItem.getDamageValue())
                      .compareNBT(ItemNBTMatcher.EXACT_MATCH, workerOwnedItem.getTag())
                      .build();
                    int slot = ItemHandlerUtils.findFirstSlotMatching(worker.getInventory(), matcher);
                    worker.getInventory().transferArmorToSlot(item.getType(), slot);
                }
            }
            else
            {
                if (ItemStackUtils.isEmpty(worker.getItemBySlot(item.getType())) && building.getBuildingLevel() >= item.getMinBuildingLevelRequired()
                      && building.getBuildingLevel() <= item.getMaxBuildingLevelRequired())
                {
                    equipment.add(item.getType());
                    int slot = ItemHandlerUtils.findFirstSlotMatching(getInventory(), item);
                    if (slot > -1)
                    {
                        worker.setItemSlot(item.getType(), worker.getInventory().getStackInSlot(slot));
                    }
                }
            }
        }
    }

    /**
     * Removes currently equipped shield
     */
    public void cleanVisibleSlots()
    {
        final ItemStack stack = worker.getItemBySlot(EquipmentSlot.OFFHAND);
        final Matcher matcher = new Matcher.Builder(stack.getItem())
          .compareDamage(stack.getDamageValue())
          .compareNBT(ItemNBTMatcher.EXACT_MATCH, stack.getTag())
          .build();
        if (stack.isEmpty() || getInventory().hasMatch(matcher))
        {
            worker.setItemSlot(EquipmentSlot.OFFHAND, ItemStackUtils.EMPTY);
        }
        worker.setItemSlot(EquipmentSlot.HEAD, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.CHEST, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.LEGS, ItemStackUtils.EMPTY);
        worker.setItemSlot(EquipmentSlot.FEET, ItemStackUtils.EMPTY);
    }
}
