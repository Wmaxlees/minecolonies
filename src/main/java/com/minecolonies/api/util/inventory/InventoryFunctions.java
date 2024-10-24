package com.minecolonies.api.util.inventory;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

/**
 * Java8 functional interfaces for inventories. Most methods will be remapping of parameters to reduce duplication. Because of erasure clashes, not
 * all combinations are supported.
 */
public final class InventoryFunctions
{
    /**
     * Private constructor to hide implicit one.
     */
    private InventoryFunctions()
    {
        /*
         * Intentionally left empty.
         */
    }

    /**
     * Search for a stack in an Inventory matching the predicate.
     *
     * @param itemHandler the handler to search in
     * @param tester      the function to use for testing slots
     * @param action      the function to use if a slot matches
     * @return true if it found a stack
     */
    public static boolean matchFirstInHandlerWithAction(
      @NotNull final IItemHandler itemHandler,
      @NotNull final Predicate<ItemStack> tester,
      @NotNull final IMatchActionResultHandler action)
    {
        return matchInHandler(
          itemHandler,
          inv -> slot -> stack ->
          {
              if (tester.test(stack))
              {
                  action.accept(itemHandler, slot);
                  return true;
              }
              return false;
          });
    }

    /**
     * Will return if it found something in the handler.
     *
     * @param handler the handler to check
     * @param tester  the function to use for testing slots
     * @return true if it found a stack
     */
    private static boolean matchInHandler(
      @Nullable final IItemHandler handler,
      @NotNull final Function<IItemHandler, Function<Integer, Predicate<ItemStack>>> tester)
    {
        if (handler == null)
        {
            return false;
        }

        final int size = handler.getSlots();
        for (int slot = 0; slot < size; slot++)
        {
            final ItemStack stack = handler.getStackInSlot(slot);
            //Unchain the function and apply it
            if (tester.apply(handler).apply(slot).test(stack))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Functional interface describing a Action that is executed ones a Match (the given ItemStack) is found in the given slot.
     */
    @FunctionalInterface
    public interface IMatchActionResult extends ObjIntConsumer<ICapabilityProvider>
    {
        /**
         * Method executed when a match has been found.
         *
         * @param provider  The itemstack that matches the predicate for the search.
         * @param slotIndex The slotindex in which this itemstack was found.
         */
        @Override
        void accept(ICapabilityProvider provider, int slotIndex);
    }

    /**
     * Functional interface describing a Action that is executed ones a Match (the given ItemStack) is found in the given slot.
     */
    @FunctionalInterface
    public interface IMatchActionResultHandler extends ObjIntConsumer<IItemHandler>
    {
        /**
         * Method executed when a match has been found.
         *
         * @param handler   The itemstack that matches the predicate for the search.
         * @param slotIndex The slotindex in which this itemstack was found.
         */
        @Override
        void accept(IItemHandler handler, int slotIndex);
    }
}
