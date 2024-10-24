package com.minecolonies.api.inventory;

import com.google.common.collect.Iterables;
import com.minecolonies.api.MinecoloniesAPIProxy;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.entity.citizen.AbstractEntityCitizen;
import com.minecolonies.api.equipment.registry.EquipmentTypeEntry;
import com.minecolonies.api.inventory.events.AbstractInventoryEvent;
import com.minecolonies.api.util.inventory.ItemHandlerUtils;
import com.minecolonies.api.util.inventory.ItemStackUtils;
import com.minecolonies.api.util.inventory.Matcher;
import com.minecolonies.api.util.inventory.params.ItemNBTMatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.minecolonies.api.research.util.ResearchConstants.CITIZEN_INV_SLOTS;
import static com.minecolonies.api.util.constant.NbtTagConstants.*;

/**
 * Basic inventory for the citizens.
 */
public class InventoryCitizen extends InventoryItemHandler implements IItemHandlerModifiable, Nameable
{
    /**
     * The returned slot if a slot hasn't been found.
     */
    private static final int NO_SLOT = -1;

    /**
     * The default inv size.
     */
    private static final int DEFAULT_INV_SIZE = 27;
    private static final int ROW_SIZE         = 9;

    /**
     * Amount of free slots
     */
    private int freeSlots = DEFAULT_INV_SIZE;

    /**
     * The inventory. (27 main inventory, 4 armor slots)
     */
    private NonNullList<ItemStack> mainInventory = NonNullList.withSize(DEFAULT_INV_SIZE, ItemStackUtils.EMPTY);
    private NonNullList<ItemStack> armorInventory = NonNullList.withSize(4, ItemStackUtils.EMPTY);

    /**
     * The index of the currently held items (0-8).
     */
    private int mainItem    = NO_SLOT;
    private int offhandItem = NO_SLOT;

    /**
     * The inventories custom name. In our case the citizens name.
     */
    private String customName;

    /**
     * The citizen which owns the inventory.
     */
    private ICitizenData citizen;

    /**
     * Creates the inventory of the citizen.
     *
     * @param title         Title of the inventory.
     * @param localeEnabled Boolean whether the inventory has a custom name.
     * @param citizen       Citizen owner of the inventory.
     */
    public InventoryCitizen(final String title, final boolean localeEnabled, final ICitizenData citizen)
    {
        this.citizen = citizen;
        if (localeEnabled)
        {
            customName = title;
        }
    }

    /**
     * Creates the inventory of the citizen.
     *
     * @param title         Title of the inventory.
     * @param localeEnabled Boolean whether the inventory has a custom name.
     */
    public InventoryCitizen(final String title, final boolean localeEnabled)
    {
        if (localeEnabled)
        {
            customName = title;
        }
    }

    /**
     * Sets the name of the inventory.
     *
     * @param customName the string to use to set the name.
     */
    public void setCustomName(final String customName)
    {
        this.customName = customName;
    }

    /**
     * Returns the item that is currently being held by citizen.
     *
     * @param hand the hand it is held in.
     * @return {@link ItemStack} currently being held by citizen.
     */
    public ItemStack getHeldItem(final InteractionHand hand)
    {
        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            return getStackInSlot(mainItem);
        }

        return getStackInSlot(offhandItem);
    }

    /**
     * Set item to be held by citizen.
     *
     * @param hand the hand it is held in.
     * @param slot Slot index with item to be held by citizen.
     */
    public boolean setHeldItem(final InteractionHand hand, final int slot)
    {
        if (slot == -1)
        {
            return false;
        }

        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            this.mainItem = slot;
            if (citizen != null)
            {
                citizen.getEntity().ifPresent(citizen -> citizen.setItemSlot(EquipmentSlot.MAINHAND, getStackInSlot(slot)));
            }
            return true;
        }

        this.offhandItem = slot;
        if (citizen != null)
        {
            citizen.getEntity().ifPresent(citizen -> citizen.setItemSlot(EquipmentSlot.OFFHAND, getStackInSlot(slot)));
        }
        return true;
    }

    public boolean setHeldItem(InteractionHand hand, Item target)
    {
        final Matcher matcher = new Matcher.Builder(target).build();
        return setHeldItem(hand, ItemHandlerUtils.findFirstSlotMatching(this, matcher::match));
    }

    public boolean setHeldItem(InteractionHand hand, Predicate<ItemStack> target)
    {
        return setHeldItem(hand, ItemHandlerUtils.findFirstSlotMatching(this, target));
    }

    /**
     * Gets slot that hold item that is being held by citizen.
     *
     * @param hand the hand it is held in.
     * @return Slot index of held item
     */
    public int getHeldItemSlot(final InteractionHand hand)
    {
        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            return mainItem;
        }

        return offhandItem;
    }

    @Override
    public int getSlots()
    {
        return this.mainInventory.size();
    }

    /**
     * Checks if the inventory is completely empty.
     *
     * @return true if the main inventory (without armor slots) is completely empty.
     */
    @Override
    public boolean isEmpty()
    {
        return freeSlots == mainInventory.size();
    }

    /**
     * Checks if the inventory is completely full.
     *
     * @return true if the main inventory (without armor slots) is completely full.
     */
    @Override
    public boolean isFull()
    {
        return freeSlots == 0;
    }

    /**
     * Resize this inventory.
     *
     * @param size       the current size.
     * @param futureSize the future size.
     */
    private void resizeInventory(final int size, final int futureSize)
    {
        if (size < futureSize)
        {
            final NonNullList<ItemStack> inv = NonNullList.withSize(futureSize, ItemStackUtils.EMPTY);

            for (int i = 0; i < mainInventory.size(); i++)
            {
                inv.set(i, mainInventory.get(i));
            }

            mainInventory = inv;
            freeSlots += futureSize - size;
        }
    }

    /**
     * Get the name of this object. For citizens this returns their name.
     *
     * @return the name of the inventory.
     */
    @NotNull
    @Override
    public Component getName()
    {
        return Component.translatable(this.hasCustomName() ? this.customName : "citizen.inventory");
    }

    /**
     * Checks if the inventory is named.
     *
     * @return true if the inventory has a custom name.
     */
    @Override
    public boolean hasCustomName()
    {
        return this.customName != null;
    }

    /**
     * Returns the stack in the given slot.
     *
     * @param index the index.
     * @return the stack.
     */
    @NotNull
    @Override
    public ItemStack getStackInSlot(final int index)
    {
        if (index == NO_SLOT)
        {
            return ItemStack.EMPTY;
        }
        if (index >= mainInventory.size())
        {
            return ItemStack.EMPTY;
        }
        else
        {
            return mainInventory.get(index);
        }
    }

    /**
     * Get the armor from a specific equipment slot.
     * @param equipmentSlot the slot to get it from.
     * @return the stack.
     */
    public ItemStack getArmorInSlot(final EquipmentSlot equipmentSlot)
    {
        if (equipmentSlot.isArmor())
        {
            return armorInventory.get(equipmentSlot.getIndex());
        }
        return ItemStack.EMPTY;
    }

    /**
     * Force an armor stack in a slot. This is for container interaction only.
     * @param equipmentSlot the slot to pick.
     * @param stack the stack to set.
     */
    public void forceArmorStackToSlot(final EquipmentSlot equipmentSlot, final ItemStack stack)
    {
        armorInventory.set(equipmentSlot.getIndex(), stack);
        if (citizen != null)
        {
            citizen.getEntity().ifPresent(citizen -> citizen.onArmorAdd(stack, equipmentSlot));
            markDirty();
        }
    }

    /**
     * Force remove armor stack from a slot. This is for container interaction only.
     * @param equipmentSlot the slot to clear.
     * @param stack the stack being removed.
     */
    public void forceClearArmorInSlot(final EquipmentSlot equipmentSlot, final ItemStack stack)
    {
        if (equipmentSlot.isArmor())
        {
            armorInventory.set(equipmentSlot.getIndex(), ItemStack.EMPTY);
            if (citizen != null)
            {
                citizen.getEntity().ifPresent(citizen -> citizen.onArmorRemove(stack, equipmentSlot));
                markDirty();
            }
        }
    }

    /**
     * Transfer from inventory slot to armor.
     * @param equipmentSlot the slot to transfer it to.
     * @param slot the slot to transfer it from.
     */
    public void transferArmorToSlot(final EquipmentSlot equipmentSlot, final int slot)
    {
        if (equipmentSlot.isArmor())
        {
            markDirty();
            final ItemStack oldArmorStack = armorInventory.get(equipmentSlot.getIndex());
            final ItemStack newArmorStack = getStackInSlot(slot);

            if (!oldArmorStack.isEmpty())
            {
                citizen.getEntity().ifPresent(citizen -> citizen.onArmorRemove(oldArmorStack, equipmentSlot));
            }

            armorInventory.set(equipmentSlot.getIndex(), newArmorStack);
            citizen.getEntity().ifPresent(citizen -> citizen.onArmorAdd(newArmorStack, equipmentSlot));
            setStackInSlot(slot, oldArmorStack);
        }
    }

    /**
     * Move armor from armor slots to inventory.
     * @param equipmentSlot the origin slot.
     */
    public void moveArmorToInventory(final EquipmentSlot equipmentSlot)
    {
        if (equipmentSlot.isArmor())
        {
            final ItemStack armorStack = armorInventory.get(equipmentSlot.getIndex());
            if (ItemHandlerUtils.insert(this, armorStack, false).isEmpty())
            {
                markDirty();
                armorInventory.set(equipmentSlot.getIndex(), ItemStack.EMPTY);
                citizen.getEntity().ifPresent(citizen -> citizen.onArmorRemove(armorStack, equipmentSlot));
            }
        }
    }

    /**
     * Damage an item within the inventory
     *
     * @param slot     slot to damage
     * @param amount   damage amount
     * @param entityIn entity which uses the item
     * @param onBroken action upon item break
     * @return true if the item broke
     */
    public <T extends LivingEntity> boolean damageInventoryItem(final int slot, int amount, @Nullable T entityIn, @Nullable Consumer<T> onBroken)
    {
        final ItemStack stack = mainInventory.get(slot);
        if (!ItemStackUtils.isEmpty(stack))
        {
            // The 4 parameter inner call from forge is for adding a callback to alter the damage caused,
            // but unlike its description does not actually damage the item(despite the same function name). So used to just calculate the damage.
            stack.hurtAndBreak(stack.getItem().damageItem(stack, amount, entityIn, onBroken), entityIn, onBroken);

            if (ItemStackUtils.isEmpty(stack))
            {
                freeSlots++;
            }
        }

        return ItemStackUtils.isEmpty(stack);
    }

    public void damageItemInHand(final InteractionHand hand, final int amount)
    {
        damageInventoryItem(getHeldItemSlot(hand), amount, citizen.getEntity().orElse(null), null);
    }

    /**
     * Shrinks an item in the given slot
     *
     * @param slot slot to shrink
     * @return true if item is empty afterwards
     */
    public boolean shrinkInventoryItem(final int slot)
    {
        final ItemStack stack = mainInventory.get(slot);
        if (!ItemStackUtils.isEmpty(stack))
        {
            stack.setCount(stack.getCount() - 1);

            if (ItemStackUtils.isEmpty(stack))
            {
                freeSlots++;
            }
        }

        return ItemStackUtils.isEmpty(stack);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(final int slot, @Nonnull final ItemStack stack, final boolean simulate)
    {
        if (stack.isEmpty())
        {
            return stack;
        }

        final Entity entity = citizen.getEntity().orElse(null);
        final int entityId = entity == null ? 0 : entity.getId();
        final ItemStack copy = stack.copy();
        final ItemStack inSlot = mainInventory.get(slot);
        final Matcher matcher = new Matcher.Builder(stack.getItem())
            .compareDamage(stack.getDamageValue())
            .compareNBT(ItemNBTMatcher.EXACT_MATCH, stack.getTag())
            .build();
        if (inSlot.getCount() >= inSlot.getMaxStackSize() || (!inSlot.isEmpty() && !matcher.match(inSlot)))
        {
            return copy;
        }

        if (inSlot.isEmpty())
        {
            if (!simulate)
            {
                markDirty();
                freeSlots--;
                mainInventory.set(slot, copy);
                MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(copy,
                        AbstractInventoryEvent.UpdateType.ADD, entityId);
            }
            return ItemStack.EMPTY;
        }

        final int avail = inSlot.getMaxStackSize() - inSlot.getCount();
        if (avail >= copy.getCount())
        {
            if (!simulate)
            {
                markDirty();
                inSlot.setCount(inSlot.getCount() + copy.getCount());
                MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(copy,
                        AbstractInventoryEvent.UpdateType.ADD, entityId);
            }
            return ItemStack.EMPTY;
        }
        else
        {
            if (!simulate)
            {
                markDirty();
                inSlot.setCount(inSlot.getCount() + avail);
                MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(inSlot.copyWithCount(avail),
                        AbstractInventoryEvent.UpdateType.ADD, entityId);
            }
            copy.setCount(copy.getCount() - avail);
            return copy;
        }
    }

    @Nonnull
    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate)
    {
        final ItemStack inSlot = mainInventory.get(slot);
        if (inSlot.isEmpty())
        {
            return ItemStack.EMPTY;
        }
        if (amount >= inSlot.getCount())
        {
            if (!simulate)
            {
                markDirty();
                freeSlots++;
                mainInventory.set(slot, ItemStack.EMPTY);
                if (citizen != null && citizen.getEntity().isPresent())
                {
                    final Entity entity = citizen.getEntity().orElse(null);
                    final int entityId = entity == null ? 0 : entity.getId();
                    MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(inSlot,
                            AbstractInventoryEvent.UpdateType.REMOVE, entityId);
                }
            }
            return inSlot;
        }
        else
        {

            final ItemStack copy = inSlot.copy();
            copy.setCount(amount);
            if (!simulate)
            {
                markDirty();
                inSlot.setCount(inSlot.getCount() - amount);
                if (ItemStackUtils.isEmpty(inSlot))
                {
                    freeSlots++;
                }
                if (citizen != null && citizen.getEntity().isPresent())
                {
                    final Entity entity = citizen.getEntity().orElse(null);
                    final int entityId = entity == null ? 0 : entity.getId();
                    MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(inSlot.copyWithCount(amount),
                            AbstractInventoryEvent.UpdateType.REMOVE, entityId);
                }  
            }
            return copy;
        }
    }

    @Override
    public int getSlotLimit(final int slot)
    {
        return 64;
    }

    @Override
    public boolean isItemValid(final int slot, @Nonnull final ItemStack stack)
    {
        return true;
    }

    /**
     * For tile entities, ensures the chunk containing the tile entity is saved to disk later - the game won't think it hasn't changed and skip it.
     */
    public void markDirty()
    {
        if (this.citizen != null)
        {
            this.citizen.markDirty(20);
        }
    }

    /**
     * Get the formatted TextComponent that will be used for the sender's username in chat.
     */
    @NotNull
    @Override
    public Component getDisplayName()
    {
        return this.hasCustomName() ? Component.literal(customName) : Component.literal(citizen.getName());
    }

    /**
     * Writes the inventory to nbt.
     *
     * @param nbtTagCompound the compound to store it in.
     */
    public void write(final CompoundTag nbtTagCompound)
    {
        if (citizen != null && citizen.getColony() != null)
        {
            final double researchEffect = citizen.getColony().getResearchManager().getResearchEffects().getEffectStrength(CITIZEN_INV_SLOTS);
            if (researchEffect > 0 && this.mainInventory.size() < DEFAULT_INV_SIZE + researchEffect)
            {
                resizeInventory(this.mainInventory.size(), (int) (DEFAULT_INV_SIZE + researchEffect));
            }
        }

        nbtTagCompound.putInt(TAG_INV_SIZE, this.mainInventory.size());

        final ListTag invTagList = new ListTag();
        freeSlots = mainInventory.size();
        for (int i = 0; i < this.mainInventory.size(); ++i)
        {
            if (!(this.mainInventory.get(i)).isEmpty())
            {
                final CompoundTag compoundNBT = new CompoundTag();
                compoundNBT.putByte("Slot", (byte) i);
                (this.mainInventory.get(i)).save(compoundNBT);
                invTagList.add(compoundNBT);
                freeSlots--;
            }
        }
        nbtTagCompound.put(TAG_INVENTORY, invTagList);

        final ListTag armorTagList = new ListTag();
        for (int i = 0; i < this.armorInventory.size(); ++i)
        {
            if (!(this.armorInventory.get(i)).isEmpty())
            {
                final CompoundTag compoundNBT = new CompoundTag();
                compoundNBT.putByte("Slot", (byte) i);
                (this.armorInventory.get(i)).save(compoundNBT);
                armorTagList.add(compoundNBT);
            }
        }
        nbtTagCompound.put(TAG_ARMOR_INVENTORY, armorTagList);
    }

    /**
     * Reads from the given compound and fills the slots in the inventory with the correct items.
     *
     * @param nbtTagCompound the compound.
     */
    public void read(final CompoundTag nbtTagCompound)
    {
        if (nbtTagCompound.contains(TAG_ARMOR_INVENTORY))
        {
            int size = nbtTagCompound.getInt(TAG_INV_SIZE);
            if (this.mainInventory.size() < size)
            {
                size -= size % ROW_SIZE;
                this.mainInventory = NonNullList.withSize(size, ItemStackUtils.EMPTY);
            }

            freeSlots = mainInventory.size();

            final ListTag nbtTagList = nbtTagCompound.getList(TAG_INVENTORY, 10);
            for (int i = 0; i < nbtTagList.size(); i++)
            {
                final CompoundTag compoundNBT = nbtTagList.getCompound(i);
                final int j = compoundNBT.getByte("Slot") & 255;
                final ItemStack itemstack = ItemStack.of(compoundNBT);

                if (!itemstack.isEmpty())
                {
                    if (j < this.mainInventory.size())
                    {
                        this.mainInventory.set(j, itemstack);
                        freeSlots--;
                        if (citizen != null && citizen.getEntity().isPresent())
                        {
                            final Entity entity = citizen.getEntity().get();
                            final int entityId = entity == null ? 0 : entity.getId();
                            MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(itemstack,
                                    AbstractInventoryEvent.UpdateType.ADD, entityId);
                        }
                    }
                }
            }

            final ListTag armorTagList = nbtTagCompound.getList(TAG_ARMOR_INVENTORY, 10);
            for (int i = 0; i < armorTagList.size(); ++i)
            {
                final CompoundTag compoundNBT = armorTagList.getCompound(i);
                final int j = compoundNBT.getByte("Slot") & 255;
                final ItemStack itemstack = ItemStack.of(compoundNBT);

                if (!itemstack.isEmpty())
                {
                    if (j < this.armorInventory.size())
                    {
                        this.armorInventory.set(j, itemstack);
                    }
                }
            }
        }
        else
        {
            final ListTag nbtTagList = nbtTagCompound.getList(TAG_INVENTORY, 10);
            if (this.mainInventory.size() < nbtTagList.getCompound(0).getInt(TAG_SIZE))
            {
                int size = nbtTagList.getCompound(0).getInt(TAG_SIZE);
                size -= size % ROW_SIZE;
                this.mainInventory = NonNullList.withSize(size, ItemStackUtils.EMPTY);
            }

            freeSlots = mainInventory.size();

            for (int i = 1; i < nbtTagList.size(); i++)
            {
                final CompoundTag compoundNBT = nbtTagList.getCompound(i);

                final int j = compoundNBT.getByte("Slot") & 255;
                final ItemStack itemstack = ItemStack.of(compoundNBT);

                if (!itemstack.isEmpty())
                {
                    if (j < this.mainInventory.size())
                    {
                        this.mainInventory.set(j, itemstack);
                        freeSlots--;
                    }
                }
            }
        }
    }

    @Override
    public void setStackInSlot(final int slot, @Nonnull final ItemStack stack)
    {
        final ItemStack originalStack = mainInventory.get(slot);
        if (!ItemStackUtils.isEmpty(stack))
        {
            if (ItemStackUtils.isEmpty(originalStack))
            {
                freeSlots--;
            }
        }
        else if (!ItemStackUtils.isEmpty(originalStack))
        {
            freeSlots++;
        }

        mainInventory.set(slot, stack);

        if (citizen != null && citizen.getEntity().isPresent())
        {
            final Entity entity = citizen.getEntity().get();
            final int entityId = entity == null ? 0 : entity.getId();
            MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(stack,
                    AbstractInventoryEvent.UpdateType.ADD, entityId);
            MinecoloniesAPIProxy.getInstance().getInventoryEventManager().fireInventoryEvent(originalStack,
                    AbstractInventoryEvent.UpdateType.REMOVE, entityId);
        }
    }

    /**
     * Get an iterable of armor and hand inventory.
     * @return the itemstack iterable.
     */
    public Iterable<ItemStack> getIterableArmorAndHandInv()
    {
        return Iterables.concat(armorInventory, List.of(getStackInSlot(mainItem), getStackInSlot(offhandItem)));
    }

    @Override
    public IItemHandler getItemHandler()
    {
        return this;
    }

    /**
     * Check if the inventory has a tool that matches the given stack
     * and the durability is not 0.
     * 
     * @param stack The stack to compare against
     * @return true if the inventory has a usable tool
     */
    public boolean hasUsableTool(ItemStack stack)
    {
        final Matcher matcher = new Matcher.Builder(stack.getItem()).build();
        ItemStack result = findFirstMatch(compareStack -> matcher.match(compareStack) && ItemStackUtils.getDurability(compareStack) > 0);

        return result != null && result != ItemStack.EMPTY;
    }

    /**
     * Damage an item within the inventory.
     * 
     * @param stack The stack use to find the item to damage
     * @param damageAmount The amount of damage to apply
     * @return the remaining damage, -1 if the operation failed
     */
    public int damageTool(ItemStack stack, int damageAmount)
    {
        IItemHandler handler = getItemHandler();
        final Matcher matcher = new Matcher.Builder(stack.getItem()).build();
        int slot = ItemHandlerUtils.findFirstSlotMatching(handler, compareStack -> matcher.match(compareStack) && ItemStackUtils.getDurability(compareStack) > 0);
        ItemStack toDamage = handler.extractItem(slot, 1, false);
        if (ItemStackUtils.isEmpty(toDamage))
        {
            return -1;
        }

        AbstractEntityCitizen entity = citizen.getEntity().get();

        // The 4 parameter inner call from forge is for adding a callback to alter the damage caused,
        // but unlike its description does not actually damage the item(despite the same function name). So used to just calculate the damage.
        toDamage.hurtAndBreak(toDamage.getItem().damageItem(toDamage, damageAmount, entity, item -> item.broadcastBreakEvent(InteractionHand.MAIN_HAND)), entity, item -> item.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        if (!ItemStackUtils.isEmpty(toDamage))
        {
            handler.insertItem(slot, toDamage, false);
        }

        return ItemStackUtils.getDurability(toDamage);
    }

    public boolean equipTool(InteractionHand hand, @NotNull EquipmentTypeEntry equipmentType, @NonNls int minimumLevel, int maximumLevel)
    {
        final int toolSlot = ItemHandlerUtils.findFirstMatchingSlotOfEquipment(this, equipmentType, minimumLevel, maximumLevel);
        if (toolSlot == -1)
        {
            return false;
        }

        return setHeldItem(hand, toolSlot);
    }

    public boolean equipTool(InteractionHand hand, Predicate<ItemStack> predicate)
    {
        final int toolSlot = ItemHandlerUtils.findFirstNonEmptySlotMatching(getItemHandler(), predicate);
        if (toolSlot == -1)
        {
            return false;
        }

        return setHeldItem(hand, toolSlot);
    }

    public boolean hasMatch(EquipmentTypeEntry toolType, int minLevel, int maxLevel)
    {
        return ItemHandlerUtils.hasEquipmentWithLevel(this, toolType, minLevel, maxLevel);
    }

    public ItemStack findFirstMatch(final @NotNull EquipmentTypeEntry equipmentTypeEntry, final int minLevel,
            final int maxLevel)
    {
        int slot = ItemHandlerUtils.findFirstMatchingSlotOfEquipment(getItemHandler(), equipmentTypeEntry, minLevel, maxLevel);
        if (slot == -1)
        {
            return ItemStack.EMPTY;
        }

        return getStackInSlot(slot);
    }

    public void equipArmor(final EquipmentSlot slot, final Matcher matcher)
    {
        final int armorSlot = ItemHandlerUtils.findFirstSlotMatching(this, matcher::match);
        if (armorSlot != -1)
        {
            transferArmorToSlot(slot, armorSlot);
        }
    }

    public boolean hasTool(@NotNull EquipmentTypeEntry toolType, int minimalLevel, int maxToolLevel)
    {
        return ItemHandlerUtils.hasEquipmentWithLevel(getItemHandler(), toolType, minimalLevel, maxToolLevel);
    }

    public long countOpenSlots()
    {
        return ItemHandlerUtils.countOpenSlots(getItemHandler());
    }

    @Override
    public BlockPos getPos()
    {
        return null;
    }

    public void removeHeldItem(InteractionHand hand)
    {
        if (hand.equals(InteractionHand.MAIN_HAND))
        {
            this.mainItem = -1;
            setStackInSlot(EquipmentSlot.MAINHAND.getIndex(), ItemStack.EMPTY);
            citizen.getEntity().ifPresent(citizen -> citizen.markEquipmentDirty());
            return;
        }

        this.offhandItem = -1;
        setStackInSlot(EquipmentSlot.OFFHAND.getIndex(), ItemStack.EMPTY);
        citizen.getEntity().ifPresent(citizen -> citizen.markEquipmentDirty());
    }
}
