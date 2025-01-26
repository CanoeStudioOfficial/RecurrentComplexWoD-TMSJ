/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.structure.sight;

import ivorius.reccomplex.RecurrentComplex;
import ivorius.mcopts.commands.CommandSplit;
import ivorius.mcopts.commands.SimpleCommand;
import ivorius.mcopts.commands.parameters.*;
import ivorius.mcopts.commands.parameters.expect.MCE;
import ivorius.reccomplex.world.gen.feature.WorldStructureGenerationData;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.05.14.
 */
public class CommandSightDelete extends CommandSplit
{
    public CommandSightDelete()
    {
        super("forget");

        add(new SimpleCommand("id", expect -> expect.skip().descriptionU("id").required())
        {
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
            {
                Parameters parameters = Parameters.of(args, expect()::declare);
                WorldStructureGenerationData generationData = WorldStructureGenerationData.get(sender.getEntityWorld());

                WorldStructureGenerationData.Entry entry = generationData.removeEntry(UUID.fromString(parameters.get(0).require()));

                if (entry == null)
                    throw RecurrentComplex.translations.commandException("commands.rcsightinfo.unknown");
                else
                    sender.sendMessage(RecurrentComplex.translations.format("commands.rcforget.success", entry.description()));
            }
        });

        add(new SimpleCommand("all", expect -> expect.then(MCE::xyz))
        {
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
            {
                Parameters parameters = Parameters.of(args, expect()::declare);
                WorldStructureGenerationData generationData = WorldStructureGenerationData.get(sender.getEntityWorld());

                BlockPos pos = parameters.get(0).to(MCP.pos(sender.getPosition(), false)).require();

                List<WorldStructureGenerationData.Entry> entries = generationData.entriesAt(pos).collect(Collectors.toList());

                entries.forEach(e -> generationData.removeEntry(e.getUuid()));

                if (entries.size() == 1)
                    sender.sendMessage(RecurrentComplex.translations.format("commands.rcforget.success", entries.get(0).description()));
                else
                    sender.sendMessage(RecurrentComplex.translations.format("commands.rcforgetall.success", entries.size()));
            }
        });

        permitFor(2);
    }
}
