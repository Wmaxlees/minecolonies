package com.minecolonies.core.commands.inventory;

import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.inventory.IInventory;
import com.minecolonies.api.util.constant.translation.CommandTranslationConstants;
import com.minecolonies.core.commands.commandTypes.IMCColonyOfficerCommand;
import com.minecolonies.core.commands.commandTypes.IMCCommand;
import com.minecolonies.core.tileentities.TileEntityColonyBuilding;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Arrays;
import java.util.List;

import static com.minecolonies.core.commands.CommandArgumentNames.*;

public class CommandBlockCachePrint implements IMCColonyOfficerCommand
{

    @Override
    public int onExecute(CommandContext<CommandSourceStack> context)
    {
        final Entity sender = context.getSource().getEntity();
        final int colonyID = IntegerArgumentType.getInteger(context, COLONYID_ARG);
        final IColony colony = IColonyManager.getInstance().getColonyByDimension(colonyID, sender == null ? Level.OVERWORLD : context.getSource().getLevel().dimension());
        if (colony == null)
        {
            context.getSource().sendSuccess(() -> Component.translatable(CommandTranslationConstants.COMMAND_COLONY_ID_NOT_FOUND, colonyID), true);
            return 0;
        }

        final Coordinates targetLocation = Vec3Argument.getCoordinates(context, POS_ARG);
        final BlockPos targetPos = targetLocation.getBlockPos(context.getSource());

        final BlockEntity blockEntity = colony.getWorld().getBlockEntity(targetPos);
        if (blockEntity == null)
        {
            context.getSource().sendSuccess(() -> Component.translatable(CommandTranslationConstants.COMMAND_ENTITY_NOT_FOUND), true);
            return 0;
        }

        final String debugString;
        if (blockEntity instanceof TileEntityColonyBuilding building)
        {
            debugString = building.getBuilding().getCacheDebugString();
        }
        else if (blockEntity instanceof IInventory inventory)
        {
            debugString = inventory.getCacheDebugString();
        }
        else
        {
            debugString = "No cache found";
        }
        
        final List<String> splitDebugString = Arrays.asList(debugString.split("\n"));
        splitDebugString.stream().forEach(line -> context.getSource().sendSuccess(() -> Component.literal(line), true));

        return 1;
    }

    @Override
    public String getName()
    {
        return "block-cache-print";
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build()
    {
        return IMCCommand.newLiteral(getName())
                 .then(IMCCommand.newArgument(COLONYID_ARG, IntegerArgumentType.integer(1))
                     .then(IMCCommand.newArgument(POS_ARG, Vec3Argument.vec3()).executes(this::checkPreConditionAndExecute)));
    }
    
}
