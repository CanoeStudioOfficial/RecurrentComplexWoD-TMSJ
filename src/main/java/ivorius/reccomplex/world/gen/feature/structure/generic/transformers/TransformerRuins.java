/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.transformers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.gson.*;
import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import ivorius.ivtoolkit.blocks.*;
import ivorius.ivtoolkit.random.BlurredValueField;
import ivorius.ivtoolkit.tools.*;
import ivorius.ivtoolkit.transform.PosTransformer;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.block.BlockGenericSolid;
import ivorius.reccomplex.block.RCBlocks;
import ivorius.reccomplex.gui.editstructure.transformers.TableDataSourceBTRuins;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.datasource.TableDataSource;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.nbt.NBTStorable;
import ivorius.reccomplex.world.gen.feature.structure.Structures;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureLiveContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureLoadContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructurePrepareContext;
import ivorius.reccomplex.world.gen.feature.structure.context.StructureSpawnContext;
import ivorius.reccomplex.world.gen.feature.structure.generic.GenericStructure;
import net.minecraft.block.*;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by lukas on 25.05.14.
 */
public class TransformerRuins extends Transformer<TransformerRuins.InstanceData>
{
    private static final List<BlockPos> neighbors;
    public static final TObjectDoubleMap<Material> stability = new TObjectDoubleHashMap<>(gnu.trove.impl.Constants.DEFAULT_CAPACITY, gnu.trove.impl.Constants.DEFAULT_LOAD_FACTOR, 1);

    static
    {
        ImmutableList.Builder<BlockPos> builder = ImmutableList.builder();

        for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                {
                    if (x != 0 || y != 0 || z != 0)
                        builder.add(new BlockPos(x, y, z));
                }

        neighbors = builder.build();

        stability.put(Material.GLASS, 0.1);
        stability.put(Material.LAVA, 2);
        stability.put(Material.CIRCUITS, 0.2);
        stability.put(Material.IRON, 2);
        stability.put(Material.WOOD, 0.3);
        stability.put(Material.CLOTH, 0.3);
    }

    public EnumFacing decayDirection;
    public float minDecay;
    public float maxDecay;
    public float decayChaos;
    public float decayValueDensity;
    public boolean gravity;

    public float blockErosion;
    public float vineGrowth;
    public float cobwebGrowth;

    public TransformerRuins()
    {
        this(null, EnumFacing.DOWN, 0.0f, 0.8f, 0.6f, 0.005f,
                true, 0.3f, 0.08f, 0.03f);
    }

    public TransformerRuins(@Nullable String id, EnumFacing decayDirection, float minDecay, float maxDecay, float decayChaos, float decayValueDensity, boolean gravity, float blockErosion, float vineGrowth, float cobwebGrowth)
    {
        super(id != null ? id : randomID(TransformerRuins.class));
        this.decayDirection = decayDirection;
        this.minDecay = minDecay;
        this.maxDecay = maxDecay;
        this.decayChaos = decayChaos;
        this.decayValueDensity = decayValueDensity;
        this.gravity = gravity;
        this.blockErosion = blockErosion;
        this.vineGrowth = vineGrowth;
        this.cobwebGrowth = cobwebGrowth;
    }

    private static int getPass(IBlockState state)
    {
        return (state.isNormalCube() || state.getMaterial() == Material.AIR) ? 0 : 1;
    }

    public static void shuffleArray(Object[] ar, Random rand)
    {
        for (int i = ar.length - 1; i > 0; i--)
        {
            int index = rand.nextInt(i + 1);

            Object a = ar[index];
            ar[index] = ar[i];
            ar[i] = a;
        }
    }

    public static int product(int[] surfaceSize)
    {
        return Arrays.stream(surfaceSize).reduce(1, (left, right) -> left * right);
    }

    @Nonnull
    protected static IBlockState randomPlacement(Random random, IBlockState stairs)
    {
        return stairs
                .withProperty(BlockStairs.FACING, EnumFacing.getHorizontal(random.nextInt(4)))
                .withProperty(BlockStairs.HALF, random.nextBoolean() ? BlockStairs.EnumHalf.BOTTOM : BlockStairs.EnumHalf.TOP);
    }

    @Override
    public boolean skipGeneration(InstanceData instanceData, StructureLiveContext context, BlockPos pos, IBlockState state, IvWorldData worldData, BlockPos sourcePos)
    {
        if (instanceData.fallingBlocks.contains(sourcePos))
            return true;

        double decay = getDecay(instanceData, sourcePos, state);

        if (decay < 0.000001)
            return false;
        else if (decay > 1)
            return true;

        return getStability(worldData, sourcePos) < decay;
    }

    public double getDecay(InstanceData instanceData, BlockPos sourcePos, IBlockState state)
    {
        return Math.pow(instanceData.getDecay(sourcePos), stability.get(state.getMaterial()));
    }

    public double getStability(IvWorldData worldData, BlockPos sourcePos)
    {
        // +1 so the lowest block has a stability < 1
        double stability = decayDirection.getFrontOffsetX() * ((sourcePos.getX() + 1) / (double) (worldData.blockCollection.getWidth() + 1))
                + decayDirection.getFrontOffsetY() * ((sourcePos.getY() + 1) / (double) (worldData.blockCollection.getHeight() + 1))
                + decayDirection.getFrontOffsetZ() * ((sourcePos.getZ() + 1) / (double) (worldData.blockCollection.getLength() + 1));
        if (stability < 0) // Negative direction, not special case
            stability += 1;
        return stability;
    }

    @Override
    public void transform(InstanceData instanceData, Phase phase, StructureSpawnContext context, IvWorldData worldData, RunTransformer transformer)
    {
        // Can't use a cache since we modify blocks
        if (phase == Phase.AFTER)
        {
            WorldServer world = context.environment.world;
            IvBlockCollection blockCollection = worldData.blockCollection;
            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};

            BlockPos lowerCoord = StructureBoundingBoxes.min(context.boundingBox);

            Map<BlockPos, NBTTagCompound> tileEntityCompounds = new HashMap<>();
            for (NBTTagCompound tileEntityCompound : worldData.tileEntities)
            {
                BlockPos key = new BlockPos(tileEntityCompound.getInteger("x"), tileEntityCompound.getInteger("y"), tileEntityCompound.getInteger("z"));

                tileEntityCompounds.put(key, tileEntityCompound);
            }

            BlockPos.MutableBlockPos dest = new BlockPos.MutableBlockPos(lowerCoord);
            for (BlockPos sourcePos : instanceData.fallingBlocks)
            {
                IBlockState source = blockCollection.getBlockState(sourcePos);

                if (!canLand(source))
                    continue;

                IvMutableBlockPos.add(context.transform.applyOn(sourcePos, dest, areaSize), lowerCoord);

                // TODO Bounce left/right
                IBlockState destState;
                while (dest.getY() > 0
                        && (destState = world.getBlockState(dest)).getBlock().isReplaceable(world, dest))
                {
                    IvMutableBlockPos.offset(dest, dest, EnumFacing.DOWN);
                }

                IvMutableBlockPos.offset(dest, dest, EnumFacing.UP);
                IBlockState state = PosTransformer.transformBlockState(source, context.transform);
                GenericStructure.setBlock(context, areaSize, dest, state, () -> tileEntityCompounds.get(sourcePos));
            }

            StructureBoundingBox dropAreaBB = context.boundingBox;
            RecurrentComplex.forgeEventHandler.disabledTileDropAreas.add(dropAreaBB);

            if (blockErosion > 0.0f || vineGrowth > 0.0f)
            {
                // Only place things on sides we KNOW we have generated already.
                StructureBoundingBox relevantBB = context.generationBB != null ? Structures.intersection(context.boundingBox, context.generationBB) : context.boundingBox;

                for (BlockPos sourceCoord : BlockAreas.mutablePositions(blockCollection.area()))
                {
                    BlockPos worldCoord = context.transform.apply(sourceCoord, areaSize).add(StructureBoundingBoxes.min(context.boundingBox));

                    if (context.includes(worldCoord))
                    {
                        IBlockState state = world.getBlockState(worldCoord);

                        if (!transformer.transformer.skipGeneration(transformer.instanceData, context, worldCoord, state, worldData, sourceCoord))
                            decayBlock(world, context.random, state, worldCoord, relevantBB);
                    }
                }
            }

            RecurrentComplex.forgeEventHandler.disabledTileDropAreas.remove(dropAreaBB);
        }
    }

    public void decayBlock(World world, Random random, IBlockState state, BlockPos pos, StructureBoundingBox boundingBox)
    {
        IBlockState newState = state;

        if (random.nextFloat() < blockErosion)
        {
            if (newState.getBlock() == Blocks.STONEBRICK
                    && newState.getProperties().get(BlockStoneBrick.VARIANT) != BlockStoneBrick.EnumType.MOSSY)
                newState = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.CRACKED);
            else if (newState.getBlock() == Blocks.SANDSTONE)
                newState = Blocks.SANDSTONE.getDefaultState().withProperty(BlockSandStone.TYPE, BlockSandStone.EnumType.DEFAULT);
        }

        newState = maybeErodeShape(random, newState, Blocks.STONEBRICK, BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.DEFAULT, Blocks.STONE_BRICK_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.PLANKS, BlockPlanks.VARIANT, BlockPlanks.EnumType.OAK, Blocks.OAK_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.PLANKS, BlockPlanks.VARIANT, BlockPlanks.EnumType.SPRUCE, Blocks.SPRUCE_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.PLANKS, BlockPlanks.VARIANT, BlockPlanks.EnumType.BIRCH, Blocks.BIRCH_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.PLANKS, BlockPlanks.VARIANT, BlockPlanks.EnumType.JUNGLE, Blocks.JUNGLE_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.PLANKS, BlockPlanks.VARIANT, BlockPlanks.EnumType.ACACIA, Blocks.ACACIA_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.PLANKS, BlockPlanks.VARIANT, BlockPlanks.EnumType.DARK_OAK, Blocks.DARK_OAK_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.SANDSTONE, null, null, Blocks.SANDSTONE_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.COBBLESTONE, null, null, Blocks.STONE_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.QUARTZ_BLOCK, null, null, Blocks.QUARTZ_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.RED_SANDSTONE, null, null, Blocks.RED_SANDSTONE_STAIRS);
        newState = maybeErodeShape(random, newState, Blocks.NETHER_BRICK, null, null, Blocks.NETHER_BRICK_STAIRS);

        if (random.nextFloat() < vineGrowth)
        {
            if (newState.getBlock() == Blocks.STONEBRICK)
                newState = Blocks.STONEBRICK.getDefaultState().withProperty(BlockStoneBrick.VARIANT, BlockStoneBrick.EnumType.MOSSY);
            else if (newState.getBlock() == Blocks.COBBLESTONE)
                newState = Blocks.MOSSY_COBBLESTONE.getDefaultState();
            else if (newState.getBlock() == Blocks.COBBLESTONE_WALL)
                newState = Blocks.COBBLESTONE_WALL.getDefaultState().withProperty(BlockWall.VARIANT, BlockWall.EnumType.MOSSY);
        }

        if (newState.getBlock() == Blocks.AIR)
        {
            newState = null;
            for (EnumFacing direction : EnumFacing.HORIZONTALS)
            {
                if (random.nextFloat() < vineGrowth
                        // Don't place vines pointing outside the structure (mostly to prevent vines in passages in mazes
                        && boundingBox.isVecInside(pos.offset(direction.getOpposite()))
                        && Blocks.VINE.canPlaceBlockOnSide(world, pos, direction))
                {
                    IBlockState downState = world.getBlockState(pos.offset(EnumFacing.DOWN));
                    downState = downState.getBlock() == Blocks.VINE ? downState : Blocks.VINE.getDefaultState();
                    downState = downState.withProperty(BlockVine.getPropertyFor(direction.getOpposite()), true);

                    int length = 1 + random.nextInt(MathHelper.floor(vineGrowth * 10.0f + 3));
                    for (int y = 0; y < length; y++)
                    {
                        BlockPos downPos = pos.offset(EnumFacing.DOWN, y);
                        if (world.getBlockState(downPos).getMaterial() == Material.AIR)
                            world.setBlockState(downPos, downState, 3);
                        else
                            break;
                    }

                    break;
                }
                else if (random.nextFloat() < cobwebGrowth && hasAirNeighbors(world, pos, 3))
                {
                    newState = null;
                    world.setBlockState(pos, Blocks.WEB.getDefaultState(), 3);
                }
            }
        }

        if (newState != null && state != newState)
            world.setBlockState(pos, newState, 3);
    }

    @Nonnull
    protected IBlockState maybeErodeShape(Random random, IBlockState newState, Block block, PropertyEnum<?> variant, Object value, Block oakStairs)
    {
        if (newState.getBlock() == block && (variant == null || newState.getProperties().get(variant) == value))
            newState = erodeShape(random, newState, oakStairs);
        return newState;
    }

    @Nonnull
    protected IBlockState erodeShape(Random random, IBlockState newState, Block stairs)
    {
        if (random.nextFloat() < blockErosion * 0.125f)
            newState = randomPlacement(random, stairs.getDefaultState());
        return newState;
    }

    public boolean hasAirNeighbors(World world, BlockPos pos, int sides)
    {
        int num = 0;
        int neg = 0;

        for (EnumFacing facing : EnumFacing.VALUES)
        {
            if (world.getBlockState(pos.offset(facing)).isNormalCube())
                num++;
            else
                neg++;

            if (num >= sides)
                return true;
            else if (neg > (6 - sides))
                return false;
        }

        throw new InternalError();
    }

    @SideOnly(Side.CLIENT)
    @Override
    public String getDisplayString()
    {
        return IvTranslations.get("reccomplex.transformer.ruins");
    }

    @SideOnly(Side.CLIENT)
    @Override
    public TableDataSource tableDataSource(TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceBTRuins(this, navigator, delegate);
    }

    @Override
    public InstanceData prepareInstanceData(StructurePrepareContext context, IvWorldData worldData)
    {
        InstanceData instanceData = new InstanceData();

        if (minDecay > 0.0f || maxDecay > 0.0f)
        {
            BlockArea sourceArea = BlockArea.areaFromSize(BlockPos.ORIGIN, StructureBoundingBoxes.size(context.boundingBox));

            double decayChaos = (context.random.nextDouble() * 0.5 + 0.5) * this.decayChaos;

            instanceData.baseDecay = context.random.nextDouble() * (maxDecay - minDecay) + minDecay;

            int[] surfaceSize = BlockAreas.side(sourceArea, decayDirection).areaSize();
            instanceData.surfaceField = new BlurredValueField(surfaceSize);
            int surfaceValues = MathHelper.floor(product(surfaceSize) * decayValueDensity + 0.5);
            for (int i = 0; i < surfaceValues; i++)
                instanceData.surfaceField.addValue((context.random.nextDouble() - context.random.nextDouble()) * decayChaos * 1.5, context.random);

            int[] volumeSize = sourceArea.areaSize();
            instanceData.volumeField = new BlurredValueField(volumeSize);
            int volumeValues = MathHelper.floor(product(volumeSize) * decayValueDensity * 0.25 + 0.5);
            for (int i = 0; i < volumeValues; i++)
                instanceData.volumeField.addValue((context.random.nextDouble() - context.random.nextDouble()) * decayChaos * 0.5, context.random);

            instanceData.clearDecayCache();
        }

        return instanceData;
    }

    @Override
    public void configureInstanceData(InstanceData instanceData, StructurePrepareContext context, IvWorldData worldData, RunTransformer transformer)
    {
        if (gravity)
        {
            IvBlockCollection blockCollection = worldData.blockCollection;
            int[] areaSize = new int[]{blockCollection.width, blockCollection.height, blockCollection.length};
            BlockPos lowerCoord = StructureBoundingBoxes.min(context.boundingBox);

            BlockPos.MutableBlockPos dest = new BlockPos.MutableBlockPos(lowerCoord);

            for (BlockPos sourcePos : BlockAreas.mutablePositions(blockCollection.area()))
            {
                IBlockState state = blockCollection.getBlockState(sourcePos);
                IvMutableBlockPos.add(context.transform.applyOn(sourcePos, dest, areaSize), lowerCoord);

                if (transformer.transformer.skipGeneration(transformer.instanceData, context, dest, state, worldData, sourcePos))
                    continue;

                if (!canFall(context, worldData, transformer, dest, sourcePos, state))
                    continue;

                double stability = getStability(worldData, sourcePos);
                double decay = getDecay(instanceData, sourcePos, state);
                double stabilitySQ = stability * stability;
                if (!(stability < decay) && stabilitySQ * stabilitySQ < decay) // Almost decay
                    instanceData.fallingBlocks.add(sourcePos.toImmutable());
            }

            Set<BlockPos> complete = new HashSet<>(product(areaSize));

            HashSet<BlockPos> connected = new HashSet<>();
            boolean[] hasFloor = new boolean[1];

            for (BlockPos startPos : BlockAreas.positions(blockCollection.area()))
            {
                if (complete.contains(startPos))
                    continue;

                hasFloor[0] = false;
                connected.clear();

                // Try to fall
                TransformerAbstractCloud.visitRecursively(Sets.newHashSet(startPos), (changed, sourcePos) ->
                {
                    if (!complete.add(sourcePos) || instanceData.fallingBlocks.contains(sourcePos))
                        return true;

                    IvMutableBlockPos.add(context.transform.applyOn(sourcePos, dest, areaSize), lowerCoord);

                    IBlockState state = blockCollection.getBlockState(sourcePos);

                    if (canFall(context, worldData, transformer, dest, sourcePos, state))
                    {
                        if (state.getBlock() == RCBlocks.genericSolid
                                && state.getValue(BlockGenericSolid.TYPE) == 0)
                            hasFloor[0] = true; // TODO Make configurable?

                        connected.add(sourcePos);
                        neighbors.stream().map(sourcePos::add).forEach(changed::add);
                    }

                    return true;
                });

                if (connected.size() > 0 && connected.size() < 200 && !hasFloor[0]) // Now we fall
                    instanceData.fallingBlocks.addAll(connected);
            }
        }
    }

    public boolean canLand(IBlockState state)
    {
        return state.getMaterial().getMobilityFlag() == EnumPushReaction.NORMAL
                // If not normal cube it will probably look weird later
                && state.isNormalCube();
    }

    public boolean canFall(StructurePrepareContext context, IvWorldData worldData, RunTransformer transformer, BlockPos.MutableBlockPos dest, BlockPos worldPos, IBlockState state)
    {
        return !transformer.transformer.skipGeneration(transformer.instanceData, context, dest, state, worldData, worldPos)
                && state.getMaterial() != Material.AIR;
    }

    @Override
    public InstanceData loadInstanceData(StructureLoadContext context, NBTBase nbt)
    {
        return new InstanceData(nbt instanceof NBTTagCompound ? (NBTTagCompound) nbt : new NBTTagCompound());
    }

    public static class InstanceData implements NBTStorable
    {
        public Double baseDecay;
        public BlurredValueField surfaceField;
        public BlurredValueField volumeField;
        public final Set<BlockPos> fallingBlocks = new HashSet<>();

        public Double[] decayCache;
        public int[] decayCacheSize;

        public InstanceData()
        {
        }

        public InstanceData(NBTTagCompound compound)
        {
            baseDecay = compound.hasKey("baseDecay") ? compound.getDouble("baseDecay") : null;
            surfaceField = compound.hasKey("field", Constants.NBT.TAG_COMPOUND)
                    ? NBTCompoundObjects.read(compound.getCompoundTag("field"), BlurredValueField::new)
                    : null;
            volumeField = compound.hasKey("volumeField", Constants.NBT.TAG_COMPOUND)
                    ? NBTCompoundObjects.read(compound.getCompoundTag("volumeField"), BlurredValueField::new)
                    : null;
            fallingBlocks.addAll(NBTTagLists.intArraysFrom(compound, "fallingBlocks").stream().map(BlockPositions::fromIntArray).collect(Collectors.toList()));
            clearDecayCache();
        }

        @Override
        public NBTBase writeToNBT()
        {
            NBTTagCompound compound = new NBTTagCompound();
            if (baseDecay != null)
                compound.setDouble("baseDecay", baseDecay);
            if (surfaceField != null)
                compound.setTag("field", NBTCompoundObjects.write(surfaceField));
            if (volumeField != null)
                compound.setTag("volumeField", NBTCompoundObjects.write(volumeField));
            NBTTagLists.writeIntArraysTo(compound, "fallingBlocks", fallingBlocks.stream().map(BlockPositions::toIntArray).collect(Collectors.toList()));
            return compound;
        }

        private Integer getIndex(BlockPos pos)
        {
            if (decayCacheSize == null)
                return null;

            if (pos.getX() < 0 || pos.getY() < 0 || pos.getZ() < 0
                    || pos.getX() >= decayCacheSize[0] || pos.getY() >= decayCacheSize[1] || pos.getZ() >= decayCacheSize[2])
                return null;

            return ((pos.getX() * decayCacheSize[1])
                    + pos.getY()) * decayCacheSize[2]
                    + pos.getZ();
        }

        public double getDecay(BlockPos pos)
        {
            if (!hasDecay())
                return 0;

            Integer index = getIndex(pos);
            if (index != null)
            {
                Double decay = decayCache[index];
                return decay != null ? decay : (decayCache[index] = calculateDecay(pos));
            }
            else
                return calculateDecay(pos);
        }

        public void clearDecayCache()
        {
            if (volumeField != null)
            {
                decayCacheSize = volumeField.getSize();
                decayCache = new Double[product(decayCacheSize)];
            }
            else
            {
                decayCacheSize = null;
                decayCache = null;
            }

//            for (BlockPos pos : new BlockArea(BlockPos.ORIGIN, new BlockPos(decayCacheSize[0] - 1, decayCacheSize[1] - 1, decayCacheSize[2] - 1)))
//                decayCache[getIndex(pos)] = calculateDecay(pos);
        }

        protected boolean hasDecay()
        {
            return baseDecay != null || surfaceField != null || volumeField != null;
        }

        protected double calculateDecay(BlockPos pos)
        {
            return (this.baseDecay != null ? this.baseDecay : 0)
                    + (this.surfaceField != null ? this.surfaceField.getValue(Math.min(pos.getX(), this.surfaceField.getSize()[0]), Math.min(pos.getY(), this.surfaceField.getSize()[1]), Math.min(pos.getZ(), this.surfaceField.getSize()[2])) : 0)
                    + (this.volumeField != null ? this.volumeField.getValue(pos.getX(), pos.getY(), pos.getZ()) : 0);
        }
    }

    public static class Serializer implements JsonDeserializer<TransformerRuins>, JsonSerializer<TransformerRuins>
    {
        private MCRegistry registry;

        public Serializer(MCRegistry registry)
        {
            this.registry = registry;
        }

        @Override
        public TransformerRuins deserialize(JsonElement jsonElement, Type par2Type, JsonDeserializationContext context)
        {
            JsonObject jsonObject = JsonUtils.asJsonObject(jsonElement, "transformerRuins");

            String id = readID(jsonObject);

            EnumFacing decayDirection = Directions.deserialize(JsonUtils.getString(jsonObject, "decayDirection", "DOWN"));
            float minDecay = JsonUtils.getFloat(jsonObject, "minDecay", 0.0f);
            float maxDecay = JsonUtils.getFloat(jsonObject, "maxDecay", 0.9f);
            float decayChaos = JsonUtils.getFloat(jsonObject, "decayChaos", 0.3f);
            float decayValueDensity = JsonUtils.getFloat(jsonObject, "decayValueDensity", 1.0f / 25.0f);

            boolean gravity = JsonUtils.getBoolean(jsonObject, "gravity", true);

            float blockErosion = JsonUtils.getFloat(jsonObject, "blockErosion", 0.0f);
            float vineGrowth = JsonUtils.getFloat(jsonObject, "vineGrowth", 0.0f);
            float cobwebGrowth = JsonUtils.getFloat(jsonObject, "cobwebGrowth", 0.0f);

            return new TransformerRuins(id, decayDirection, minDecay, maxDecay, decayChaos, decayValueDensity, gravity, blockErosion, vineGrowth, cobwebGrowth);
        }

        @Override
        public JsonElement serialize(TransformerRuins transformer, Type par2Type, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("id", transformer.id());

            jsonObject.addProperty("decayDirection", Directions.serialize(transformer.decayDirection));
            jsonObject.addProperty("minDecay", transformer.minDecay);
            jsonObject.addProperty("maxDecay", transformer.maxDecay);
            jsonObject.addProperty("decayChaos", transformer.decayChaos);
            jsonObject.addProperty("decayValueDensity", transformer.decayValueDensity);
            jsonObject.addProperty("gravity", transformer.gravity);

            jsonObject.addProperty("blockErosion", transformer.blockErosion);
            jsonObject.addProperty("vineGrowth", transformer.vineGrowth);
            jsonObject.addProperty("cobwebGrowth", transformer.cobwebGrowth);

            return jsonObject;
        }
    }
}
