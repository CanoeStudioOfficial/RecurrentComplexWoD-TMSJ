/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.generation;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.client.rendering.MazeVisualizationContext;
import ivorius.reccomplex.files.SimpleLeveledRegistry;
import ivorius.reccomplex.gui.editstructure.gentypes.TableDataSourceNaturalGeneration;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.datasource.TableDataSource;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.utils.presets.PresettedList;
import ivorius.reccomplex.utils.presets.PresettedObjects;
import ivorius.reccomplex.world.gen.feature.WorldStructureGenerationData;
import ivorius.reccomplex.world.gen.feature.selector.*;
import ivorius.reccomplex.world.gen.feature.structure.Placer;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.generic.WeightedBiomeMatcher;
import ivorius.reccomplex.world.gen.feature.structure.generic.WeightedDimensionMatcher;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.SelectivePlacer;
import ivorius.reccomplex.world.gen.feature.structure.generic.presets.BiomeMatcherPresets;
import ivorius.reccomplex.world.gen.feature.structure.generic.presets.DimensionMatcherPresets;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by lukas on 07.10.14.
 */
public class NaturalGeneration extends GenerationType implements EnvironmentalSelection<NaturalStructureSelector.Category>
{
    private static Gson gson = createGson();

    public final PresettedList<WeightedBiomeMatcher> biomeWeights = new PresettedList<>(BiomeMatcherPresets.instance(), null);
    public final PresettedList<WeightedDimensionMatcher> dimensionWeights = new PresettedList<>(DimensionMatcherPresets.instance(), null);

    private Double generationWeight;

    public String generationCategory;

    public SelectivePlacer placer;

    public SpawnLimitation spawnLimitation;

    public NaturalGeneration()
    {
        this(null, "decoration");

        biomeWeights.setPreset("overworld");
        dimensionWeights.setPreset("overworld");
        placer =  new SelectivePlacer();
    }

    public NaturalGeneration(@Nullable String id, String generationCategory)
    {
        super(id != null ? id : randomID(NaturalGeneration.class));
        this.generationCategory = generationCategory;
    }

    public static Gson createGson()
    {
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeAdapter(NaturalGeneration.class, new NaturalGeneration.Serializer());
        builder.registerTypeAdapter(WeightedBiomeMatcher.class, new WeightedBiomeMatcher.Serializer());
        builder.registerTypeAdapter(WeightedDimensionMatcher.class, new WeightedDimensionMatcher.Serializer());

        return builder.create();
    }

    public static Gson getGson()
    {
        return gson;
    }

    public static NaturalGeneration deserializeFromVersion1(JsonObject jsonObject, JsonDeserializationContext context)
    {
        String generationCategory = JsonUtils.getString(jsonObject, "generationCategory");

        NaturalGeneration naturalGeneration = new NaturalGeneration(readID(new JsonObject()), generationCategory);
        if (jsonObject.has("generationBiomes"))
        {
            WeightedBiomeMatcher[] infos = gson.fromJson(jsonObject.get("generationBiomes"), WeightedBiomeMatcher[].class);
            naturalGeneration.biomeWeights.setContents(Arrays.asList(infos));
        }
        else
            naturalGeneration.biomeWeights.setPreset("overworld");

        naturalGeneration.dimensionWeights.setPreset("overworld");

        naturalGeneration.placer = SelectivePlacer.Serializer.readLegacyPlacer(context, JsonUtils.getJsonObject(jsonObject, "generationY", new JsonObject()));

        return naturalGeneration;
    }

    public static CachedStructureSelectors<MixingStructureSelector<NaturalGeneration, NaturalStructureSelector.Category>> selectors(StructureRegistry registry)
    {
        return registry.module(Cache.class).selectors;
    }

    public Double getGenerationWeight()
    {
        return generationWeight;
    }

    public void setGenerationWeight(Double generationWeight)
    {
        this.generationWeight = generationWeight;
    }

    @Override
    public double getGenerationWeight(WorldProvider provider, Biome biome)
    {
        return getActiveGenerationWeight() * StructureSelector.generationWeight(provider, biome, this.biomeWeights, this.dimensionWeights);
    }

    @Override
    public NaturalStructureSelector.Category generationCategory()
    {
        return NaturalStructureSelector.CATEGORY_REGISTRY.getActive(generationCategory);
    }

    public double getActiveGenerationWeight()
    {
        return generationWeight != null ? generationWeight : 1.0;
    }

    public boolean hasDefaultWeight()
    {
        return generationWeight == null;
    }

    public boolean hasLimitations()
    {
        return spawnLimitation != null;
    }

    public SpawnLimitation getLimitations()
    {
        return spawnLimitation;
    }

    @Nonnull
    @Override
    public String id()
    {
        return id;
    }

    @Override
    public void setID(@Nonnull String id)
    {
        this.id = id;
    }

    @Override
    public String displayString()
    {
        return IvTranslations.get("reccomplex.generationInfo.natural");
    }

    @Nullable
    @Override
    public Placer placer()
    {
        return placer;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public TableDataSource tableDataSource(MazeVisualizationContext mazeVisualizationContext, TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceNaturalGeneration(navigator, delegate, this);
    }

    public static class SpawnLimitation
    {
        public int maxCount = 1;
        public Context context = Context.DIMENSION;

        public boolean areResolved(World world, String structureID)
        {
            return WorldStructureGenerationData.get(world).getEntriesByID(structureID).size() < maxCount;
        }

        public enum Context
        {
            @SerializedName("dimension")
            DIMENSION,
        }
    }

    public static class Serializer implements JsonSerializer<NaturalGeneration>, JsonDeserializer<NaturalGeneration>
    {
        @Override
        public NaturalGeneration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = JsonUtils.asJsonObject(json, "naturalGenerationInfo");

            String id = readID(jsonObject);

            String generationCategory = JsonUtils.getString(jsonObject, "generationCategory");

            NaturalGeneration naturalGeneration = new NaturalGeneration(id, generationCategory);

            naturalGeneration.placer = SelectivePlacer.gson.fromJson(json, SelectivePlacer.class);

            if (jsonObject.has("generationWeight"))
                naturalGeneration.generationWeight = JsonUtils.getDouble(jsonObject, "generationWeight");

            PresettedObjects.read(jsonObject, gson, naturalGeneration.biomeWeights, "biomeWeightsPreset", "generationBiomes", new TypeToken<ArrayList<WeightedBiomeMatcher>>() {}.getType());
            PresettedObjects.read(jsonObject, gson, naturalGeneration.dimensionWeights, "dimensionWeightsPreset", "generationDimensions", new TypeToken<ArrayList<WeightedDimensionMatcher>>() {}.getType());

            if (jsonObject.has("spawnLimitation"))
                naturalGeneration.spawnLimitation = context.deserialize(jsonObject.get("spawnLimitation"), SpawnLimitation.class);

            return naturalGeneration;
        }

        @Override
        public JsonElement serialize(NaturalGeneration src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject jsonObject = (JsonObject) SelectivePlacer.gson.toJsonTree(src.placer);

            jsonObject.addProperty("id", src.id);

            jsonObject.addProperty("generationCategory", src.generationCategory);
            if (src.generationWeight != null)
                jsonObject.addProperty("generationWeight", src.generationWeight);

            PresettedObjects.write(jsonObject, gson, src.biomeWeights, "biomeWeightsPreset", "generationBiomes");
            PresettedObjects.write(jsonObject, gson, src.dimensionWeights, "dimensionWeightsPreset", "generationDimensions");

            if (src.spawnLimitation != null)
                jsonObject.add("spawnLimitation", context.serialize(src.spawnLimitation));

            return jsonObject;
        }
    }

    public static class Cache extends SimpleLeveledRegistry.Module<StructureRegistry>
    {
        protected CachedStructureSelectors<MixingStructureSelector<NaturalGeneration, NaturalStructureSelector.Category>> selectors;

        @Override
        public void setRegistry(StructureRegistry registry)
        {
            selectors = new CachedStructureSelectors<>((biome, worldProvider) ->
                    new MixingStructureSelector<>(registry.activeMap(), worldProvider, biome, NaturalGeneration.class));
        }

        @Override
        public void invalidate()
        {
            selectors.clear();
        }
    }
}
