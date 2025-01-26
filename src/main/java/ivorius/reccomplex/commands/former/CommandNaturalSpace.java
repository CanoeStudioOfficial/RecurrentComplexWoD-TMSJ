/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.former;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockSurfaceArea;
import ivorius.ivtoolkit.blocks.BlockSurfacePos;
import ivorius.ivtoolkit.world.MockWorld;
import ivorius.reccomplex.block.BlockGenericSpace;
import ivorius.reccomplex.block.RCBlocks;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.CommandVirtual;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.mcopts.commands.CommandExpecting;
import ivorius.mcopts.commands.parameters.*;
import ivorius.mcopts.commands.parameters.expect.Expect;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandNaturalSpace extends CommandExpecting implements CommandVirtual
{
    public static int sidesClosed(MockWorld world, BlockPos coord, BlockArea area)
    {
        int sides = 0;

        BlockPos lower = area.getLowerCorner();
        BlockPos higher = area.getHigherCorner();

        if (sideClosed(world, new BlockPos(lower.getX(), coord.getY(), coord.getZ()), coord.getX() - lower.getX(), 1, 0, 0))
            sides++;
        if (sideClosed(world, new BlockPos(higher.getX(), coord.getY(), coord.getZ()), higher.getX() - coord.getX(), -1, 0, 0))
            sides++;
        if (sideClosed(world, new BlockPos(coord.getX(), coord.getY(), lower.getZ()), coord.getZ() - lower.getZ(), 0, 0, 1))
            sides++;
        if (sideClosed(world, new BlockPos(coord.getX(), coord.getY(), higher.getZ()), higher.getZ() - coord.getZ(), 0, 0, -1))
            sides++;

        return sides;
    }

    public static boolean sideClosed(MockWorld world, BlockPos coord, int iterations, int xP, int yP, int zP)
    {
        for (int i = 0; i < iterations; i++)
        {
            BlockPos pos = coord.add(xP * i, yP * i, zP * i);
            IBlockState blockState = world.getBlockState(pos);

            if (!blockState.getBlock().isReplaceable(world, pos))
                return true;
        }

        return false;
    }

    public static void placeNaturalAir(MockWorld world, BlockArea area, int floorDistance, int maxClosedSides)
    {
        BlockGenericSpace spaceBlock = RCBlocks.genericSpace;

        BlockPos lowerPoint = area.getLowerCorner();
        BlockPos higherPoint = area.getHigherCorner();

        Set<BlockPos> set = new HashSet<>();

        for (BlockSurfacePos surfaceCoord : BlockSurfaceArea.from(area))
        {
            int safePoint = lowerPoint.getY();

            for (int y = higherPoint.getY(); y >= lowerPoint.getY(); y--)
            {
                IBlockState blockState = world.getBlockState(surfaceCoord.blockPos(y));

                if ((blockState.getMaterial() != Material.AIR && blockState.getBlock() != spaceBlock) || sidesClosed(world, surfaceCoord.blockPos(y), area) >= maxClosedSides)
                {
                    boolean isFloor = blockState == RCBlocks.genericSolid.getDefaultState();
                    safePoint = y + (isFloor ? 1 : floorDistance);
                    break;
                }
            }

            for (int y = safePoint; y <= higherPoint.getY(); y++)
                set.add(surfaceCoord.blockPos(y));

            if (safePoint > lowerPoint.getY())
            {
                for (int y = lowerPoint.getY(); y <= higherPoint.getY(); y++)
                {
                    IBlockState blockState = world.getBlockState(surfaceCoord.blockPos(y));

                    if ((blockState.getMaterial() != Material.AIR && blockState.getBlock() != spaceBlock) || sidesClosed(world, surfaceCoord.blockPos(y), area) >= maxClosedSides)
                    {
                        safePoint = y - 1;
                        break;
                    }
                }
            }

            for (int y = lowerPoint.getY(); y <= safePoint; y++)
                set.add(surfaceCoord.blockPos(y));
        }

        set.forEach(pos ->
        {
            BlockPos down = pos.down();
            BlockPos down2 = pos.down(2);
            world.setBlockState(pos,
                    pos.getY() > lowerPoint.getY() && !set.contains(down)
                            && world.getBlockState(down).getBlock().isReplaceable(world, down) && world.getBlockState(down2).getBlock().isReplaceable(world, down2)
                            && new BlockArea(pos.subtract(new Vec3i(2, 0, 2)), pos.add(new Vec3i(2, 0, 2))).stream().allMatch(set::contains)
                            ? spaceBlock.getDefaultState().withProperty(BlockGenericSpace.TYPE, 1)
                            : spaceBlock.getDefaultState()
            );
        });
    }

    @Override
    public String getName()
    {
        return "space";
    }

    @Override
    public void expect(Expect expect)
    {
        expect
                .named("distance-to-floor", "d").any("3", "2", "1")
                .named("max-closed-sides", "s").any("3", "4", "5");
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MockWorld world, ICommandSender sender, String[] args) throws CommandException
    {
        SelectionOwner selectionOwner = RCCommands.getSelectionOwner(sender, null, true);
        RCCommands.assertSize(sender, selectionOwner);
        BlockArea area = selectionOwner.getSelection();

        Parameters parameters = Parameters.of(args, expect()::declare);

        int floorDistance = parameters.get("distance-to-floor").to(NaP::asInt).optional().orElse(0) + 1;
        int maxClosedSides = parameters.get("max-closed-sides").to(NaP::asInt).optional().orElse(3);

        placeNaturalAir(world, area, floorDistance, maxClosedSides);
    }
}
