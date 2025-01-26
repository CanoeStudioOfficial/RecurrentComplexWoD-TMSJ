/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.former;

import com.google.common.collect.Sets;
import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockStates;
import ivorius.ivtoolkit.math.IvShapeHelper;
import ivorius.ivtoolkit.world.MockWorld;
import ivorius.mcopts.commands.CommandExpecting;
import ivorius.mcopts.commands.parameters.MCP;
import ivorius.mcopts.commands.parameters.NaP;
import ivorius.mcopts.commands.parameters.Parameters;
import ivorius.mcopts.commands.parameters.expect.Expect;
import ivorius.mcopts.commands.parameters.expect.MCE;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.capability.SelectionOwner;
import ivorius.reccomplex.commands.CommandVirtual;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.reccomplex.commands.parameters.RCP;
import ivorius.reccomplex.commands.parameters.expect.RCE;
import ivorius.reccomplex.utils.expression.PositionedBlockExpression;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by lukas on 09.06.14.
 */
public class CommandFill extends CommandExpecting implements CommandVirtual
{
    public static final int MAX_FLOOD = 50 * 50 * 50;

    public static void runShape(RCP.Shape shape, BlockArea area, Consumer<BlockPos> consumer) throws CommandException
    {
        BlockPos p1 = area.getPoint1();
        BlockPos p2 = area.getPoint2();

        switch (shape)
        {
            case cube:
                for (BlockPos pos : area)
                    consumer.accept(pos);
                break;
            case sphere:
            {
                double[] spheroidOrigin = new double[]{(p1.getX() + p2.getX()) * 0.5, (p1.getY() + p2.getY()) * 0.5, (p1.getZ() + p2.getZ()) * 0.5};
                int[] areaSize = area.areaSize();
                double[] spheroidSize = new double[]{areaSize[0] * 0.5, areaSize[1] * 0.5, areaSize[2] * 0.5};

                for (BlockPos pos : area)
                {
                    double[] coordPoint = new double[]{pos.getX(), pos.getY(), pos.getZ()};
                    if (IvShapeHelper.isPointInSpheroid(coordPoint, spheroidOrigin, spheroidSize))
                        consumer.accept(pos);
                }
                break;
            }
            default:
                throw new InternalError();
        }
    }

    public static void setFrom(MockWorld world, List<IBlockState> dst, BlockPos pos)
    {
        IBlockState state = dst.get(world.rand().nextInt(dst.size()));
        world.setBlockState(pos, state, 2);
    }

    @Override
    public String getName()
    {
        return RCConfig.commandPrefix + "fill";
    }

    @Override
    public void expect(Expect expect)
    {
        expect
                .then(MCE::block)
                .then(MCE::block).descriptionU("source expression").optional().repeat()
                .named("metadata", "m").then(RCE::metadata)
                .named("shape", "s").then(RCE::shape)
                .named("flood", "f").words(RCE::directionExpression).descriptionU("direction expression")
        ;
    }

    public int getRequiredPermissionLevel()
    {
        return 2;
    }

    @Override
    public void execute(MockWorld world, ICommandSender sender, String[] args) throws CommandException
    {
        Parameters parameters = Parameters.of(args, expect()::declare);

        Block dstBlock = parameters.get(0).to(MCP::block, sender).require();
        int[] dstMeta = parameters.get("metadata").to(RCP::metadatas).optional().orElse(new int[1]);
        List<IBlockState> dst = IntStream.of(dstMeta).mapToObj(m -> BlockStates.fromMetadata(dstBlock, m)).collect(Collectors.toList());

        RCP.Shape shape = parameters.get("shape").to(RCP::shape).optional().orElse(RCP.Shape.cube);

        PositionedBlockExpression matcher = parameters.get(1).rest(NaP::join).orElse("")
                .to(RCP::expression, new PositionedBlockExpression(RecurrentComplex.specialRegistry)).require();

        SelectionOwner selectionOwner = RCCommands.getSelectionOwner(sender, null, true);
        RCCommands.assertSize(sender, selectionOwner);

        if (parameters.has("flood"))
        {
            List<EnumFacing> directions = parameters.get("flood").orElse("").to(RCP::directions).require();

            List<BlockPos> dirty = new ArrayList<>();
            Set<BlockPos> visited = Sets.newHashSet(dirty);

            runShape(shape, selectionOwner.getSelection(), dirty::add);

            while (!dirty.isEmpty())
            {
                BlockPos pos = dirty.remove(dirty.size() - 1);

                for (EnumFacing facing : directions)
                {
                    BlockPos offset = pos.offset(facing);
                    if (matcher.evaluate(() -> PositionedBlockExpression.Argument.at(world, offset))
                            && visited.add(offset))
                        dirty.add(offset);
                }

                if (visited.size() > MAX_FLOOD)
                    throw new CommandException("Area too big to flood!");
            }

            visited.forEach(p -> setFrom(world, dst, p));
        }
        else
        {
            // TODO Can't freeze because the lighting update won't get sent
//            HeightMapFreezer freezer = HeightMapFreezer.freeze(BlockAreas.toBoundingBox(selectionOwner.getSelection()), sender.getEntityWorld());
            runShape(shape, selectionOwner.getSelection(), pos ->
            {
                if (matcher.evaluate(() -> PositionedBlockExpression.Argument.at(world, pos)))
                {
                    setFrom(world, dst, pos);
//                freezer.markBlock(pos, state);
                }
            });
//        freezer.melt();
        }
    }
}
