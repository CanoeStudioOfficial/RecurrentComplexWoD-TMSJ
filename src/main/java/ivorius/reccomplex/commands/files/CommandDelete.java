/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.files;

import com.google.common.collect.Lists;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.commands.RCCommands;
import ivorius.mcopts.commands.CommandExpecting;
import ivorius.mcopts.commands.parameters.NaP;
import ivorius.mcopts.commands.parameters.Parameters;
import ivorius.mcopts.commands.parameters.expect.Expect;
import ivorius.reccomplex.commands.parameters.RCP;
import ivorius.reccomplex.commands.parameters.expect.RCE;
import ivorius.reccomplex.files.loading.LeveledRegistry;
import ivorius.reccomplex.files.loading.ResourceDirectory;
import ivorius.reccomplex.files.saving.FileSaverAdapter;
import ivorius.reccomplex.utils.RawResourceLocation;
import ivorius.reccomplex.utils.algebra.ExpressionCache;
import ivorius.reccomplex.utils.expression.ResourceExpression;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * Created by lukas on 03.08.14.
 */
public class CommandDelete extends CommandExpecting
{
    @Override
    public String getName()
    {
        return RCConfig.commandPrefix + "delete";
    }

    public int getRequiredPermissionLevel()
    {
        return 4;
    }

    @Override
    public void expect(Expect expect)
    {
        expect
                .next(RecurrentComplex.saver.keySet()).descriptionU("file type").required()
                .next(params -> params.get(0).tryGet().map(RecurrentComplex.saver::get).map(a -> a.getRegistry().ids())).descriptionU("resource expression").repeat()
                .named("directory", "d").then(RCE::resourceDirectory).required();
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender commandSender, String[] args) throws CommandException
    {
        Parameters parameters = Parameters.of(args, expect()::declare);

        String adapterID = parameters.get(0).require();

        if (!RecurrentComplex.saver.has(adapterID))
            throw RecurrentComplex.translations.commandException("commands.rcsave.noregistry");

        ResourceDirectory directory = parameters.get("directory").to(RCP::resourceDirectory).require();
        Optional<FileSaverAdapter<?>> adapterOptional = Optional.ofNullable(RecurrentComplex.saver.get(adapterID));
        Collection<String> ids = Lists.newArrayList(adapterOptional.map(a -> a.getRegistry().ids()).orElse(Collections.emptySet()));

        ResourceExpression resourceExpression = ExpressionCache.of(new ResourceExpression(id -> adapterOptional.map(a -> a.getRegistry().has(id)).orElse(false)),
                parameters.get(1).rest(NaP::join).require());

        for (String id : ids)
        {
            if (!resourceExpression.test(new RawResourceLocation(adapterOptional.map(a -> a.getRegistry().status(id).getDomain()).orElseThrow(IllegalStateException::new), id)))
                continue;

            RCCommands.informDeleteResult(RecurrentComplex.saver.tryDeleteWithID(directory.toPath(), adapterID, id), commandSender, adapterID, id, directory);

            // Could also predict changes and just reload those for the file but eh.
            RCCommands.tryReload(RecurrentComplex.loader, LeveledRegistry.Level.CUSTOM);
            RCCommands.tryReload(RecurrentComplex.loader, LeveledRegistry.Level.SERVER);
        }
    }
}
