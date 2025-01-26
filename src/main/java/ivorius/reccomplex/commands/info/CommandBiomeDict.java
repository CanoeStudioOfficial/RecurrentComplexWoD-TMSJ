/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.commands.info;

import com.google.common.collect.Lists;
import ivorius.reccomplex.RCConfig;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.commands.RCTextStyle;
import ivorius.mcopts.commands.CommandSplit;
import ivorius.mcopts.commands.SimpleCommand;
import ivorius.mcopts.commands.parameters.*;
import ivorius.mcopts.commands.parameters.expect.MCE;
import ivorius.reccomplex.commands.structure.CommandSearchStructure;
import ivorius.mcopts.translation.ServerTranslations;
import ivorius.reccomplex.commands.structure.StructureSearch;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Created by lukas on 03.08.14.
 */
public class CommandBiomeDict extends CommandSplit
{
    public CommandBiomeDict()
    {
        super(RCConfig.commandPrefix + "biome");

        add(new SimpleCommand("search", expect -> expect.skip().descriptionU("terms").required())
        {
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
            {
                Parameters parameters = Parameters.of(args, expect()::declare);

                List<String> terms = parameters.get(0).varargsList().require();

                CommandSearchStructure.postResultMessage("Biomes:", sender,
                        RCTextStyle::biome,
                        CommandSearchStructure.search(Biome.REGISTRY.getKeys(), loc -> StructureSearch.searchRank(terms, keywords(loc, Biome.REGISTRY.getObject(loc))))
                );
            }
        });

        add(new SimpleCommand("types", expect -> expect.then(MCE::biome).required())
        {
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
            {
                Parameters parameters = Parameters.of(args, expect()::declare);

                Biome biome = parameters.get(0).to(MCP::biome).require();

                sender.sendMessage(RecurrentComplex.translations.format("commands.biomedict.get", RCTextStyle.biome(biome),
                        ServerTranslations.join(Lists.newArrayList(BiomeDictionary.getTypes(biome)).stream()
                                .map(RCTextStyle::biomeType).toArray())
                ));
            }
        });

        add(new SimpleCommand("list", expect -> expect.then(MCE::biomeType).required())
        {
            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
            {
                Parameters parameters = Parameters.of(args, expect()::declare);

                BiomeDictionary.Type type = parameters.get(0).to(MCP::biomeDictionaryType).require();

                sender.sendMessage(RecurrentComplex.translations.format("commands.biomedict.list", RCTextStyle.biomeType(type),
                        ServerTranslations.join(Lists.newArrayList(BiomeDictionary.getBiomes(type))
                                .stream().map(RCTextStyle::biome).toArray())
                ));
            }
        });

        permitFor(2);
    }

    @Nonnull
    public static Collection<String> keywords(ResourceLocation id, Biome biome)
    {
        return Arrays.asList(id.toString(), biome.getBiomeName());
    }
}
