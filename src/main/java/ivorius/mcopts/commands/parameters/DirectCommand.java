/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.mcopts.commands.parameters;

import ivorius.mcopts.commands.SimpleCommand;
import ivorius.mcopts.commands.parameters.expect.Expect;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.function.Consumer;

/**
 * Created by lukas on 01.06.17.
 */
public class DirectCommand extends SimpleCommand
{
    public Consumer<ICommandSender> consumer;

    public DirectCommand(String name, Consumer<ICommandSender> consumer)
    {
        super(name);
        this.consumer = consumer;
    }

    public DirectCommand(String name, String usage, Consumer<Expect> expector, Consumer<ICommandSender> consumer)
    {
        super(name, usage, expector);
        this.consumer = consumer;
    }

    public DirectCommand(String name, Consumer<Expect> expector, Consumer<ICommandSender> consumer)
    {
        super(name, expector);
        this.consumer = consumer;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
    {
        consumer.accept(sender);
    }
}
