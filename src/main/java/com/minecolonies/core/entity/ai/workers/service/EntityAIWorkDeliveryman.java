package com.minecolonies.core.entity.ai.workers.service;

import com.minecolonies.api.colony.buildings.IBuilding;
import com.minecolonies.api.colony.buildings.workerbuildings.IWareHouse;
import com.minecolonies.api.colony.interactionhandling.ChatPriority;
import com.minecolonies.api.colony.requestsystem.location.ILocation;
import com.minecolonies.api.colony.requestsystem.request.IRequest;
import com.minecolonies.api.colony.requestsystem.requestable.IRequestable;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.Delivery;
import com.minecolonies.api.colony.requestsystem.requestable.deliveryman.IDeliverymanRequestable;
import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.inventory.IInventory;
import com.minecolonies.api.inventory.InventoryCitizen;
import com.minecolonies.api.tileentities.AbstractTileEntityColonyBuilding;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.minecolonies.core.tileentities.TileEntityRack;
import com.minecolonies.api.util.Log;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.inventory.InventoryUtils;
import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;
import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.colony.buildings.modules.WorkerBuildingModule;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingDeliveryman;
import com.minecolonies.core.colony.interactionhandling.PosBasedInteraction;
import com.minecolonies.core.colony.interactionhandling.StandardInteraction;
import com.minecolonies.core.colony.jobs.JobDeliveryman;
import com.minecolonies.core.colony.requestsystem.requests.StandardRequests.DeliveryRequest;
import com.minecolonies.core.colony.requestsystem.requests.StandardRequests.PickupRequest;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.api.util.constant.Constants.TICKS_SECOND;
import static com.minecolonies.api.util.constant.StatisticsConstants.ITEMS_DELIVERED;
import static com.minecolonies.api.util.constant.TranslationConstants.*;

/**
 * Delivers item at needs.
 */
public class EntityAIWorkDeliveryman extends AbstractEntityAIInteract<JobDeliveryman, BuildingDeliveryman>
{
    /**
     * Min distance the worker should have to the warehouse to make any decisions.
     */
    private static final int MIN_DISTANCE_TO_WAREHOUSE = 5;

    /**
     * Wait 5 seconds for the worker to decide what to do.
     */
    private static final int DECISION_DELAY = TICKS_SECOND * 5;

    /**
     * Wait a few ticks for the worker to decide what to pick up.
     */
    private static final int PICKUP_DELAY = 5;

    /**
     * The inventory's slot which is held in hand.
     */
    private static final int SLOT_HAND = 0;

    /**
     * Completing a request with a priority of at least PRIORITY_FORCING_DUMP will force a dump.
     */
    private static final int PRIORITY_FORCING_DUMP = 10;

    /**
     * Delivery icon
     */
    private final static VisibleCitizenStatus DELIVERING =
      new VisibleCitizenStatus(new ResourceLocation(Constants.MOD_ID, "textures/icons/work/delivery.png"), "com.minecolonies.gui.visiblestatus.delivery");

    /**
     * Render meta backpack.
     */
    public static final String RENDER_META_BACKPACK = "backpack";

    /**
     * Amount of stacks left to gather from the inventory at the gathering step.
     */
    private int currentSlot = 0;

    /**
     * Amount of stacks the worker already kept in the current gathering process.
     */
    private List<ItemStorage> alreadyKept = new ArrayList<>();

    /**
     * Initialize the deliveryman and add all his tasks.
     *
     * @param deliveryman the job he has.
     */
    public EntityAIWorkDeliveryman(@NotNull final JobDeliveryman deliveryman)
    {
        super(deliveryman);
        super.registerTargets(
          /*
           * Check if tasks should be executed.
           */
          new AITarget(IDLE, () -> START_WORKING, 1),
          new AITarget(START_WORKING, this::checkIfExecute, this::decide, DECISION_DELAY),
          new AITarget(PREPARE_DELIVERY, this::prepareDelivery, STANDARD_DELAY),
          new AITarget(DELIVERY, this::deliver, STANDARD_DELAY),
          new AITarget(PICKUP, this::pickup, PICKUP_DELAY),
          new AITarget(DUMPING, this::dump, TICKS_SECOND)

        );
        worker.setCanPickUpLoot(true);
    }

    @Override
    protected void updateRenderMetaData()
    {
        worker.setRenderMetadata(worker.getInventory().isEmpty() ? "" : RENDER_META_BACKPACK);
    }

    @Override
    public Class<BuildingDeliveryman> getExpectedBuildingClass()
    {
        return BuildingDeliveryman.class;
    }

    /**
     * Pickup items from a hut that has requested a pickup.
     *
     * @return the next state to go to.
     */
    private IAIState pickup()
    {
        setDelay(WALK_DELAY);
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof PickupRequest))
        {
            // The current task has changed since the Decision-state. Restart.
            return START_WORKING;
        }

        if (cannotHoldMoreItems())
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);

        final BlockPos pickupTarget = currentTask.getRequester().getLocation().getInDimensionLocation();
        if (pickupTarget != BlockPos.ZERO && !worker.isWorkerAtSiteWithMove(pickupTarget, MIN_DISTANCE_TO_WAREHOUSE))
        {
            return PICKUP;
        }

        final IBuilding pickupBuilding = building.getColony().getBuildingManager().getBuilding(pickupTarget);
        if (pickupBuilding == null)
        {
            job.finishRequest(false);
            return START_WORKING;
        }

        if (pickupFromBuilding(pickupBuilding))
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            job.finishRequest(true);

            if (currentTask.getRequest().getPriority() >= PRIORITY_FORCING_DUMP)
            {
                return DUMPING;
            }
            else
            {
                return START_WORKING;
            }
        }
        else if (worker.getInventory().isFull())
        {
            this.alreadyKept = new ArrayList<>();
            this.currentSlot = 0;
            return DUMPING;
        }

        setDelay(5);
        currentSlot++;
        return PICKUP;
    }

    /**
     * Gather not needed Items from building.
     *
     * @param building building to gather it from.
     * @return true when finished.
     */
    private boolean pickupFromBuilding(@NotNull final IBuilding building)
    {
        if (cannotHoldMoreItems() || worker.getInventory().isFull())
        {
            return false;
        }

        if (building.isEmpty())
        {
            return true;
        }

        ItemStack stack = building.findFirstMatch(itemStack -> workerRequiresItem(building, itemStack, alreadyKept) > 0);
        if (stack.isEmpty())
        {
            return true;
        }

        final int amount = workerRequiresItem(building, stack, alreadyKept);
        final Matcher matcher = new Matcher.Builder(stack.getItem())
            .compareDamage(stack.getDamageValue())
            .compareNBT(ItemNBTMatcher.EXACT_MATCH, stack.getTag())
            .build();
        final ItemStack activeStack = building.extractStack(matcher, amount, ItemCountType.MATCH_COUNT_EXACTLY, false);
        worker.getInventory().insert(activeStack, false);
        building.markDirty();
        worker.decreaseSaturationForContinuousAction();

        // The worker gets a little bit of exp for every itemstack he grabs.
        worker.getCitizenExperienceHandler().addExperience(0.01D);
        worker.getInventory().setHeldItem(InteractionHand.MAIN_HAND, activeStack.getItem());
        return false;
    }

    /**
     * Check if the worker can hold that much items. It depends on his building level. Level 1: 1 stack Level 2: 2 stacks, 4 stacks, 8, unlimited. That's 2^buildingLevel-1.
     *
     * @return whether this deliveryman can hold more items
     */
    private boolean cannotHoldMoreItems()
    {
        if (building.getBuildingLevel() >= building.getMaxBuildingLevel())
        {
            return false;
        }
        return worker.getInventory().countMatches(stack -> !stack.isEmpty()) >= Math.pow(2, building.getBuildingLevel() - 1.0D) + 1;
    }

    /**
     * Check if worker of a certain building requires the item now. Or the builder for the current task.
     *
     * @param building         the building to check for.
     * @param stack            the stack to stack with.
     * @param localAlreadyKept already kept resources.
     * @return the amount which can get dumped.
     */
    public static int workerRequiresItem(final IBuilding building, final ItemStack stack, final List<ItemStorage> localAlreadyKept)
    {
        return building.buildingRequiresCertainAmountOfItem(stack, localAlreadyKept, false);
    }

    /**
     * Dump the inventory into the warehouse.
     *
     * @return the next state to go to.
     */
    private IAIState dump()
    {
        final @Nullable IWareHouse warehouse = getAndCheckWareHouse();
        if (warehouse == null)
        {
            return START_WORKING;
        }

        if (!worker.isWorkerAtSiteWithMove(warehouse.getPosition(), MIN_DISTANCE_TO_WAREHOUSE))
        {
            setDelay(WALK_DELAY);
            return DUMPING;
        }

        warehouse.getTileEntity().dumpInventoryIntoWareHouse(worker.getInventory());
        worker.getInventory().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);

        return START_WORKING;
    }

    /**
     * Gets the colony's warehouse for the Deliveryman.
     *
     * @return the warehouse. null if no warehouse registered.
     */
    @Nullable
    private IWareHouse getAndCheckWareHouse()
    {
        return job.findWareHouse();
    }

    /**
     * Deliver the items to the hut. TODO: Current precondition: The dman's inventory may only consist of the requested itemstack.
     *
     * @return the next state.
     */
    private IAIState deliver()
    {
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();

        if (!(currentTask instanceof DeliveryRequest))
        {
            // The current task has changed since the Decision-state.
            // Since prepareDelivery() was called earlier, go dumping first and then restart.
            return DUMPING;
        }

        worker.getCitizenData().setVisibleStatus(DELIVERING);

        final ILocation targetBuildingLocation = ((Delivery) currentTask.getRequest()).getTarget();
        if (!targetBuildingLocation.isReachableFromLocation(worker.getLocation()))
        {
            Log.getLogger().info(worker.getCitizenColonyHandler().getColony().getName() + ": " + worker.getName() + ": Can't inter dimension yet: ");
            return START_WORKING;
        }

        if (!worker.isWorkerAtSiteWithMove(targetBuildingLocation.getInDimensionLocation(), MIN_DISTANCE_TO_WAREHOUSE))
        {
            setDelay(WALK_DELAY);
            return DELIVERY;
        }

        final BlockEntity tileEntity = world.getBlockEntity(targetBuildingLocation.getInDimensionLocation());

        if (!(tileEntity instanceof TileEntityColonyBuilding) || ((AbstractTileEntityColonyBuilding) tileEntity).getBuilding() == null)
        {
            // TODO: Non-Colony deliveries are unsupported yet. Fix that at some point in time.
            job.finishRequest(true);
            return START_WORKING;
        }

        final IBuilding targetBuilding = ((AbstractTileEntityColonyBuilding) tileEntity).getBuilding();

        boolean success = true;
        boolean extracted = false;
        final InventoryCitizen workerInventory = worker.getInventory();
        final List<ItemStorage> itemsToDeliver =
          job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask).stream().map(r -> new ItemStorage(r.getRequest().getStack())).collect(Collectors.toList());

        for (int i = 0; i < workerInventory.getSlots(); i++)
        {
            if (workerInventory.getStackInSlot(i).isEmpty())
            {
                continue;
            }

            if (!itemsToDeliver.contains(new ItemStorage(workerInventory.getStackInSlot(i))))
            {
                continue;
            }

            final ItemStack stack = workerInventory.extractItem(i, Integer.MAX_VALUE, false);
            final int count = stack.getCount();
            if (ItemStackUtils.isEmpty(stack))
            {
                continue;
            }

            extracted = true;
            final ItemStack insertionResultStack;

            // TODO: Please only push items into the target that were actually requested.
            if (targetBuilding instanceof AbstractBuilding)
            {
                insertionResultStack = targetBuilding.forceInsert(stack, targetBuilding::isItemStackInRequest, false);
            }
            else
            {
                // Buildings that are not inherently part of the request system, but just receive a delivery, cannot have their items replaced.
                // Therefore, the keep-predicate always returns true.
                insertionResultStack = targetBuilding.insert(stack, false);
            }

            if (!ItemStackUtils.isEmpty(insertionResultStack))
            {
                // A stack was replaced (meaning the inventory didn't have enough space).

                if (ItemStack.matches(insertionResultStack, stack) && worker.getCitizenData() != null)
                {
                    // The replaced stack is the same as the one we tried to put into the inventory.
                    // Meaning, replacing failed.
                    success = false;

                    if (targetBuilding.hasModule(WorkerBuildingModule.class))
                    {
                        worker.getCitizenData()
                          .triggerInteraction(new PosBasedInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NAMEDCHESTFULL,
                            targetBuilding.getFirstModuleOccurance(WorkerBuildingModule.class).getFirstCitizen().getName()),
                            ChatPriority.IMPORTANT,
                            Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL),
                            targetBuilding.getID()));
                    }
                    else
                    {
                        worker.getCitizenData()
                          .triggerInteraction(new PosBasedInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL,
                            Component.literal(" :" + targetBuilding.getSchematicName())),
                            ChatPriority.IMPORTANT,
                            Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_CHESTFULL),
                            targetBuildingLocation.getInDimensionLocation()));
                    }
                }

                //Insert the result back into the inventory so we do not lose it.
                workerInventory.insertItem(i, insertionResultStack, false);
            }
            worker.getCitizenColonyHandler()
              .getColony()
              .getStatisticsManager()
              .incrementBy(ITEMS_DELIVERED, count - insertionResultStack.getCount(), worker.getCitizenColonyHandler().getColony().getDay());
        }

        if (!extracted)
        {
            // This can only happen if the dman's inventory was completely empty.
            // Let the retry-system handle this case.
            worker.decreaseSaturationForContinuousAction();
            worker.getInventory().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);
            job.finishRequest(false);

            // No need to go dumping in this case.
            return START_WORKING;
        }

        worker.getCitizenExperienceHandler().addExperience(1.5D);
        worker.decreaseSaturationForContinuousAction();
        worker.getInventory().setHeldItem(InteractionHand.MAIN_HAND, SLOT_HAND);
        job.finishRequest(true);
        return success ? START_WORKING : DUMPING;
    }

    /**
     * Prepare deliveryman for delivery. Check if the building still needs the item and if the required items are still in the warehouse.
     *
     * @return the next state to go to.
     */
    private IAIState prepareDelivery()
    {
        final IRequest<? extends IRequestable> currentTask = job.getCurrentTask();
        if (!(currentTask instanceof DeliveryRequest))
        {
            // The current task has changed since the Decision-state.
            // Restart.
            return START_WORKING;
        }

        final List<IRequest<? extends Delivery>> taskList = job.getTaskListWithSameDestination((IRequest<? extends Delivery>) currentTask);
        final List<ItemStack> alreadyInInv = new ArrayList<>();
        IRequest<? extends Delivery> nextPickUp = null;

        int parallelDeliveryCount = 0;
        for (final IRequest<? extends Delivery> task : taskList)
        {
            parallelDeliveryCount++;
            final Matcher matcher = new Matcher.Builder(task.getRequest().getStack().getItem())
                .compareDamage(task.getRequest().getStack().getDamageValue())
                .compareNBT(ItemNBTMatcher.IMPORTANT_KEYS, task.getRequest().getStack().getTag())
                .build();
            int totalCount = worker.getInventory().countMatches(matcher);
            int hasCount = 0;
            for (final ItemStack stack : alreadyInInv)
            {
                if (matcher.match(stack))
                {
                    hasCount += stack.getCount();
                }
            }

            if (totalCount < hasCount + task.getRequest().getStack().getCount())
            {
                nextPickUp = task;
                break;
            }
            else
            {
                alreadyInInv.add(task.getRequest().getStack());
            }
        }

        if (nextPickUp == null || parallelDeliveryCount > 1 + (getSecondarySkillLevel() / 5))
        {
            return DELIVERY;
        }

        final ILocation location = nextPickUp.getRequest().getStart();

        if (!location.isReachableFromLocation(worker.getLocation()))
        {
            job.finishRequest(false);
            return START_WORKING;
        }

        if (walkToBlock(location.getInDimensionLocation()))
        {
            return PREPARE_DELIVERY;
        }

        if (getInventory().isFull())
        {
            return DUMPING;
        }

        final BlockEntity tileEntity = world.getBlockEntity(location.getInDimensionLocation());
        job.addConcurrentDelivery(nextPickUp.getId());
        if (gatherIfInTileEntity(tileEntity, nextPickUp.getRequest().getStack()))
        {
            return PREPARE_DELIVERY;
        }

        if (parallelDeliveryCount > 1)
        {
            job.removeConcurrentDelivery(nextPickUp.getId());
            return DELIVERY;
        }

        job.finishRequest(false);
        job.removeConcurrentDelivery(nextPickUp.getId());
        return START_WORKING;
    }

    /**
     * Finds the first @see ItemStack the type of {@code is}. It will be taken from the chest and placed in the worker inventory. Make sure that the worker stands next the chest to
     * not break immersion. Also make sure to have inventory space for the stack.
     *
     * @param entity the tileEntity chest or building or rack.
     * @param is     the itemStack.
     * @return true if found the stack.
     */
    public boolean gatherIfInTileEntity(final BlockEntity entity, final ItemStack is)
    {
        Log.getLogger().info("EntityAIWorkDeliveryman.gatherIfInTileEntity: entity = " + entity.getBlockPos() + ", is = " + is.getDisplayName().getString());
        if (ItemStackUtils.isEmpty(is))
        {
            return false;
        }

        Matcher matcher = new Matcher.Builder(is.getItem())
            .compareDamage(is.getDamageValue())
            .compareNBT(ItemNBTMatcher.IMPORTANT_KEYS, is.getTag())
            .build();
        if (!(entity instanceof IInventory inventory))
        {
            return false;
        }
        if (inventory.countMatches(matcher) >= is.getCount())
        {
            return InventoryUtils.transfer(inventory, worker.getInventory(),
                    stack -> !ItemStackUtils.isEmpty(stack) && ItemStackUtils.compareItemStack(matcher, stack),
                    is.getCount(), ItemCountType.MATCH_COUNT_EXACTLY);
        }

        return false;
    }

    /**
     * Check the wareHouse for the next task.
     *
     * @return the next AiState to go to.
     */
    private IAIState decide()
    {
        worker.getCitizenData().setVisibleStatus(VisibleCitizenStatus.WORKING);
        final IRequest<? extends IDeliverymanRequestable> currentTask = job.getCurrentTask();
        if (currentTask == null)
        {
            // If there are no deliveries/pickups pending, just loiter around the warehouse.
            if (!worker.isWorkerAtSiteWithMove(getAndCheckWareHouse().getPosition(), MIN_DISTANCE_TO_WAREHOUSE))
            {
                setDelay(WALK_DELAY);
                return START_WORKING;
            }
            else
            {
                if (!worker.getInventory().isEmpty())
                {
                    return DUMPING;
                }
                else
                {
                    return START_WORKING;
                }
            }
        }
        if (currentTask instanceof DeliveryRequest)
        {
            // Before a delivery can be made, the inventory first needs to be dumped.
            if (!worker.getInventory().isEmpty())
            {
                return DUMPING;
            }
            else
            {
                return PREPARE_DELIVERY;
            }
        }
        else
        {
            return PICKUP;
        }
    }

    /**
     * Check if the deliveryman code should be executed. More concretely if he has a warehouse to work at.
     *
     * @return false if should continue as planned.
     */
    private boolean checkIfExecute()
    {
        final IWareHouse wareHouse = getAndCheckWareHouse();
        if (wareHouse != null)
        {
            worker.getCitizenData().setWorking(true);
            if (wareHouse.getTileEntity() == null)
            {
                return false;
            }
            {
                return true;
            }
        }

        worker.getCitizenData().setWorking(false);
        if (worker.getCitizenData() != null)
        {
            worker.getCitizenData()
              .triggerInteraction(new StandardInteraction(Component.translatable(COM_MINECOLONIES_COREMOD_JOB_DELIVERYMAN_NOWAREHOUSE),
                ChatPriority.BLOCKING));
        }
        return false;
    }
}
