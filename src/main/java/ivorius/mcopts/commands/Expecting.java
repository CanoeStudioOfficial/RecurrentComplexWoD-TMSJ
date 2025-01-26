/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.mcopts.commands;

import ivorius.mcopts.commands.parameters.Parameters;
import ivorius.mcopts.commands.parameters.expect.Expect;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by lukas on 04.06.17.
 */
public interface Expecting extends ICommand
{
    default Expect expect()
    {
        return Parameters.expect().then(this::expect);
    }

    void expect(Expect expect);

    @Nonnull
    default String getUsage(ICommandSender var1)
    {
        return String.format("%s %s", getName(), expect().usage());
    }

    @Nonnull
    default List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos pos)
    {
        return expect().get(server, sender, args, pos);
    }
}
