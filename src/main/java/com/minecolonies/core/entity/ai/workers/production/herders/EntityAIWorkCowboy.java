package com.minecolonies.core.entity.ai.workers.production.herders;

import com.minecolonies.api.crafting.ItemStorage;
import com.minecolonies.api.entity.ai.statemachine.AITarget;
import com.minecolonies.api.entity.ai.statemachine.states.IAIState;
import com.minecolonies.api.entity.citizen.VisibleCitizenStatus;
import com.minecolonies.api.items.ModItems;
import com.minecolonies.api.util.constant.Constants;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemCountType;
import com.minecolonies.core.colony.buildings.workerbuildings.BuildingCowboy;
import com.minecolonies.core.colony.jobs.JobCowboy;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import static com.minecolonies.api.entity.ai.statemachine.states.AIWorkerState.*;
import static com.minecolonies.core.colony.buildings.workerbuildings.BuildingCowboy.MILKING_AMOUNT;

/**
 * The AI behind the {@link JobCowboy} for Breeding, Killing and Milking Cows.
 */
public class EntityAIWorkCowboy extends AbstractEntityAIHerder<JobCowboy, BuildingCowboy>
{
    /**
     * Bucket metadata.
     */
    public static final String RENDER_META_BUCKET = "bucket";
    public static final String RENDER_META_BOWL   = "bowl";

    /**
     * Herd cow icon
     */
    private final static VisibleCitizenStatus HERD_COW =
      new VisibleCitizenStatus(new ResourceLocation(Constants.MOD_ID, "textures/icons/work/cowboy.png"), "com.minecolonies.gui.visiblestatus.cowboy");

    /**
     * Min wait between failed milking attempts.
     */
    private static final int MILK_COOL_DOWN = 10;

    private int milkCoolDown;
    private int stewCoolDown;

    /**
     * Creates the abstract part of the AI. Always use this constructor!
     *
     * @param job the job to fulfill
     */
    public EntityAIWorkCowboy(@NotNull final JobCowboy job)
    {
        super(job);
        super.registerTargets(
          new AITarget(COWBOY_MILK, this::milkCows, 1),
          new AITarget(COWBOY_STEW, this::milkMooshrooms, 1)
        );
    }

    @Override
    protected void updateRenderMetaData()
    {
        String renderMeta = getState() == IDLE ? "" : RENDER_META_WORKING;
        final Matcher bucketMatcher = new Matcher.Builder(Items.BUCKET).build();
        final Matcher largeEmptyBottleMatcher = new Matcher.Builder(ModItems.large_empty_bottle).build();
        final Matcher bowlMatcher = new Matcher.Builder(Items.BOWL).build();
        if (worker.getInventory().hasMatch(bucketMatcher) || worker.getInventory().hasMatch(largeEmptyBottleMatcher))
        {
            renderMeta += RENDER_META_BUCKET;
        }
        if (worker.getInventory().hasMatch(bowlMatcher))
        {
            renderMeta += RENDER_META_BOWL;
        }
        worker.setRenderMetadata(renderMeta);
    }

    @Override
    public Class<BuildingCowboy> getExpectedBuildingClass()
    {
        return BuildingCowboy.class;
    }

    @Override
    public IAIState decideWhatToDo()
    {
        final IAIState result = super.decideWhatToDo();

        if (milkCoolDown > 0)
        {
            --milkCoolDown;
        }
        else if (building != null && building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToMilk() && result.equals(START_WORKING))
        {
            return COWBOY_MILK;
        }

        if (stewCoolDown > 0)
        {
            --stewCoolDown;
        }
        else if (building != null && building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToStew() && result.equals(START_WORKING))
        {
            return COWBOY_STEW;
        }

        return result;
    }

    @NotNull
    @Override
    public List<ItemStack> getExtraItemsNeeded()
    {
        final List<ItemStack> list = super.getExtraItemsNeeded();
        if (building != null && building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToMilk() &&
              !searchForAnimals(a -> a instanceof Cow && !(a instanceof MushroomCow)).isEmpty())
        {
            final ItemStack stack = building.getMilkInputItem().copy();
            stack.setCount(building.getSetting(MILKING_AMOUNT).getValue());
            list.add(stack);
        }
        if (building != null && building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).canTryToStew() &&
              !searchForAnimals(a -> a instanceof MushroomCow).isEmpty())
        {
            list.add(new ItemStack(Items.BOWL));
        }
        return list;
    }

    /**
     * Makes the Cowboy "Milk" the cows (Honestly all he does is swap an empty bucket for a milk bucket, there's no actual "Milk" method in {@link Cow}
     *
     * @return The next {@link IAIState}
     */
    private IAIState milkCows()
    {
        worker.getCitizenData().setVisibleStatus(HERD_COW);

        final Matcher outputMatcher = new Matcher.Builder(building.getMilkOutputItem().getItem()).build();
        if (!worker.getInventory().hasMatch(outputMatcher))
        {
            final Matcher inputMatcher = new Matcher.Builder(building.getMilkInputItem().getItem()).build();
            if (building.hasMatch(inputMatcher) && !walkToBuilding())
            {
                checkAndTransferFromHut(building.getMilkInputItem());
            }
            else
            {
                milkCoolDown = MILK_COOL_DOWN;
                return DECIDE;
            }
        }

        final Cow cow = searchForAnimals(a -> a instanceof Cow && !(a instanceof MushroomCow) && !a.isBaby()).stream()
                          .map(a -> (Cow) a).findFirst().orElse(null);

        if (cow == null)
        {
            milkCoolDown = MILK_COOL_DOWN;
            return DECIDE;
        }

        if (equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(building.getMilkInputItem())) && !walkingToAnimal(cow))
        {
            if (worker.getInventory().insert(building.getMilkOutputItem(), false).isEmpty())
            {
                building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).onMilked();
                worker.getInventory().removeHeldItem(InteractionHand.MAIN_HAND);
                equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(building.getMilkOutputItem()));
                final Matcher matcher = new Matcher.Builder(building.getMilkInputItem().getItem()).build();
                worker.getInventory().extractStack(matcher, 1, ItemCountType.MATCH_COUNT_EXACTLY, false);
            }

            incrementActionsDoneAndDecSaturation();
            worker.getCitizenExperienceHandler().addExperience(1.0);
            return INVENTORY_FULL;
        }

        return DECIDE;
    }

    /**
     * Makes the Cowboy "Milk" the mooshrooms
     *
     * @return The next {@link IAIState}
     */
    private IAIState milkMooshrooms()
    {
        worker.getCitizenData().setVisibleStatus(HERD_COW);

        final Matcher bowlMatcher = new Matcher.Builder(Items.BOWL).build();
        if (!worker.getInventory().hasMatch(bowlMatcher))
        {
            if (building.hasMatch(bowlMatcher) && !walkToBuilding())
            {
                checkAndTransferFromHut(new ItemStack(Items.BOWL, 1));
            }
            else
            {
                stewCoolDown = MILK_COOL_DOWN;
                return DECIDE;
            }
        }

        final MushroomCow mooshroom = searchForAnimals(a -> a instanceof MushroomCow && !a.isBaby()).stream()
                                        .map(a -> (MushroomCow) a).findFirst().orElse(null);

        if (mooshroom == null)
        {
            stewCoolDown = MILK_COOL_DOWN;
            return DECIDE;
        }

        if (equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(new ItemStack(Items.BOWL))) && !walkingToAnimal(mooshroom))
        {
            final FakePlayer fakePlayer = FakePlayerFactory.getMinecraft((ServerLevel) worker.level);
            fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOWL));
            if (mooshroom.mobInteract(fakePlayer, InteractionHand.MAIN_HAND).equals(InteractionResult.CONSUME))
            {
                if (worker.getInventory().insert(fakePlayer.getMainHandItem(), false).isEmpty())
                {
                    building.getFirstModuleOccurance(BuildingCowboy.HerdingModule.class).onStewed();
                    worker.getInventory().removeHeldItem(InteractionHand.MAIN_HAND);
                    equipItem(InteractionHand.MAIN_HAND, Collections.singletonList(fakePlayer.getMainHandItem()));
                    worker.getInventory().extractStack(bowlMatcher, 1, ItemCountType.MATCH_COUNT_EXACTLY, false);
                }
                fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            }

            incrementActionsDoneAndDecSaturation();
            worker.getCitizenExperienceHandler().addExperience(1.0);
            return INVENTORY_FULL;
        }

        return DECIDE;
    }

    @Override
    public double getButcheringAttackDamage()
    {
        return Math.max(1.0, getPrimarySkillLevel() / 10.0);
    }

    @Override
    protected IAIState breedAnimals()
    {
        worker.getCitizenData().setVisibleStatus(HERD_COW);
        return super.breedAnimals();
    }

    @Override
    protected IAIState butcherAnimals()
    {
        worker.getCitizenData().setVisibleStatus(HERD_COW);
        return super.butcherAnimals();
    }
}
