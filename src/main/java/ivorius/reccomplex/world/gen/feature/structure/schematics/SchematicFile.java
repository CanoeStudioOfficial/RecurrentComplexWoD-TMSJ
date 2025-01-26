/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.schematics;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.blocks.BlockStates;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.transform.Mover;
import ivorius.ivtoolkit.transform.PosTransformer;
import ivorius.ivtoolkit.util.IvStreams;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.temp.RCMover;
import ivorius.reccomplex.temp.RCPosTransformer;
import ivorius.reccomplex.utils.accessor.RCAccessorEntity;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.DataFixesManager;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Stream;

/**
 * Created by lukas on 29.09.14.
 */
public class SchematicFile
{
    public final List<NBTTagCompound> entityCompounds = new ArrayList<>();
    public final List<NBTTagCompound> tileEntityCompounds = new ArrayList<>();
    public short width, height, length;
    public Short weOriginX, weOriginY, weOriginZ;
    public IBlockState[] blockStates;

    public SchematicFile()
    {
    }

    public SchematicFile(short width, short height, short length)
    {
        this.width = width;
        this.height = height;
        this.length = length;
        this.blockStates = new IBlockState[width * height * length];
    }

    public SchematicFile(NBTTagCompound tagCompound) throws UnsupportedSchematicFormatException
    {
        final DataFixer fixer = DataFixesManager.createFixer();

        String materials = tagCompound.getString("Materials");
        if (!(materials.equals("Alpha")))
            throw new UnsupportedSchematicFormatException(materials);

        width = tagCompound.getShort("Width");
        height = tagCompound.getShort("Height");
        length = tagCompound.getShort("Length");

        if (tagCompound.hasKey("WEOriginX", Constants.NBT.TAG_SHORT))
            weOriginX = tagCompound.getShort("WEOriginX");
        if (tagCompound.hasKey("WEOriginY", Constants.NBT.TAG_SHORT))
            weOriginY = tagCompound.getShort("WEOriginY");
        if (tagCompound.hasKey("WEOriginZ", Constants.NBT.TAG_SHORT))
            weOriginZ = tagCompound.getShort("WEOriginZ");

        byte[] metadatas = tagCompound.getByteArray("Data");
        byte[] blockIDs = tagCompound.getByteArray("Blocks");
        byte[] addBlocks = tagCompound.getByteArray("AddBlocks");

        SchematicMapping schematicMapping = tagCompound.hasKey(SchematicMapping.COMPOUND_KEY, Constants.NBT.TAG_COMPOUND)
                ? new SchematicMapping(tagCompound.getCompoundTag(SchematicMapping.COMPOUND_KEY))
                : null;

        this.blockStates = new IBlockState[blockIDs.length];
        for (int i = 0; i < blockIDs.length; i++)
        {
            int blockID = blockIDs[i] & 0xff;

            if (addBlocks.length >= (blockIDs.length + 1) / 2)
            {
                boolean lowerNybble = (i & 1) == 0;
                blockID |= lowerNybble ? ((addBlocks[i >> 1] & 0x0F) << 8) : ((addBlocks[i >> 1] & 0xF0) << 4);
            }

            int metadata = metadatas[i];

            try
            {
                Block block = schematicMapping != null
                        ? schematicMapping.blockFromID(blockID)
                        : Block.getBlockById(blockID);
                this.blockStates[i] = BlockStates.fromMetadata(block, metadata);
            }
            catch (Exception ex)
            {
                RecurrentComplex.logger.error("Invalid metadata in schematic file: " + blockID + ":" + metadata, ex);
                this.blockStates[i] = Blocks.AIR.getDefaultState();
            }
        }

        NBTTagList entities = tagCompound.getTagList("Entities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < entities.tagCount(); i++)
            entityCompounds.add(fixer.process(FixTypes.ENTITY, entities.getCompoundTagAt(i)));

        NBTTagList tileEntities = tagCompound.getTagList("TileEntities", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < tileEntities.tagCount(); i++)
            tileEntityCompounds.add(fixer.process(FixTypes.BLOCK_ENTITY, tileEntities.getCompoundTagAt(i)));
    }

    public int getBlockIndex(BlockPos pos)
    {
        return pos.getX() + (pos.getY() * length + pos.getZ()) * width;
    }

    public IBlockState getBlockState(BlockPos coord)
    {
        if (coord.getX() < 0 || coord.getY() < 0 || coord.getZ() < 0 || coord.getX() >= width || coord.getY() >= height || coord.getZ() >= length)
            return Blocks.AIR.getDefaultState();

        return blockStates[getBlockIndex(coord)];
    }

    public boolean setBlockState(BlockPos coord, IBlockState state)
    {
        if (coord.getX() < 0 || coord.getY() < 0 || coord.getZ() < 0 || coord.getX() >= width || coord.getY() >= height || coord.getZ() >= length)
            return false;

        blockStates[getBlockIndex(coord)] = state;

        return true;
    }

    public boolean shouldRenderSide(BlockPos coord, EnumFacing side)
    {
        return !getBlockState(coord.offset(side)).isOpaqueCube();
    }

    public void generate(World world, BlockPos pos)
    {
        generate(world, pos, AxisAlignedTransform2D.ORIGINAL);
    }

    /**
     * Mirror is applied first
     */
    public void generate(World world, BlockPos origin, AxisAlignedTransform2D transform)
    {
        int[] areaSize = {width, height, length};

        Map<BlockPos, TileEntity> tileEntities = new HashMap<>();
        for (NBTTagCompound tileTagCompound : tileEntityCompounds)
        {
            BlockPos src = RCMover.getTileEntityPos(tileTagCompound);
            BlockPos dest = transform.apply(src, areaSize).add(origin);

            tileTagCompound = RCMover.setTileEntityPos(tileTagCompound, dest);

            TileEntity tileEntity = TileEntity.create(world, tileTagCompound);
            if (tileEntity != null)
                tileEntities.put(src, tileEntity);
        }

        for (int pass = 0; pass < 2; pass++)
        {
            for (BlockPos sourcePos : area())
            {
                int index = getBlockIndex(sourcePos);
                IBlockState blockState = PosTransformer.transformBlockState(blockStates[index], transform);

                if (blockState != null && getPass(blockState) == pass)
                {
                    BlockPos worldPos = origin.add(transform.apply(sourcePos, areaSize));
                    world.setBlockState(worldPos, blockState, 2);

                    TileEntity tileEntity = tileEntities.get(sourcePos);
                    if (tileEntity != null)
                    {
                        world.setBlockState(worldPos, blockState, 2); // Second time to ensure state, see BlockFurnace

                        RCPosTransformer.transformAdditionalData(tileEntity, transform, areaSize);
                        RCMover.moveAdditionalData(tileEntity, origin);

                        world.setTileEntity(worldPos, tileEntity);
                        tileEntity.updateContainingBlockInfo();
                    }
                }
            }
        }

        for (NBTTagCompound entityCompound : entityCompounds)
        {
            Entity entity = EntityList.createEntityFromNBT(entityCompound, world);
            if (entity != null)
            {
                RCAccessorEntity.setEntityUniqueID(entity, UUID.randomUUID());
                PosTransformer.transformEntityPos(entity, transform, areaSize);
                Mover.moveEntity(entity, origin);
                world.spawnEntity(entity);
            }
        }
    }

    @Nonnull
    public BlockArea area()
    {
        return BlockArea.areaFromSize(BlockPos.ORIGIN, new int[]{width, height, length});
    }

    private int getPass(IBlockState blockState)
    {
        return (blockState.isNormalCube() || blockState.getMaterial() == Material.AIR) ? 0 : 1;
    }

    public void writeToNBT(NBTTagCompound tagCompound)
    {
        tagCompound.setString("Materials", "Alpha");

        tagCompound.setShort("Width", width);
        tagCompound.setShort("Height", height);
        tagCompound.setShort("Length", length);

        if (weOriginX != null)
            tagCompound.setShort("WEOriginX", weOriginX);
        if (weOriginY != null)
            tagCompound.setShort("WEOriginY", weOriginY);
        if (weOriginZ != null)
            tagCompound.setShort("WEOriginZ", weOriginZ);

        tagCompound.setByteArray("Data", IvStreams.toByteArray(Stream.of(blockStates).mapToInt(BlockStates::toMetadata)));

        byte[] blockIDs = new byte[blockStates.length];
        byte[] addBlocks = new byte[(blockStates.length + 1) / 2];
        SchematicMapping schematicMapping = new SchematicMapping();
        for (int i = 0; i < blockStates.length; i++)
        {
            Block block = blockStates[i].getBlock();
            int blockID = getBlockID(block);
            schematicMapping.putBlock(blockID, block);

            blockIDs[i] = (byte) (blockID & 0xff);
            boolean lowerNybble = (i & 1) == 0;
            addBlocks[i >> 1] |= lowerNybble ? (byte) ((blockID >> 8) & 0x0F) : (byte) ((blockID >> 4) & 0xF0);
        }
        tagCompound.setByteArray("Blocks", blockIDs);
        tagCompound.setByteArray("AddBlocks", addBlocks);
        tagCompound.setTag(SchematicMapping.COMPOUND_KEY, schematicMapping.writeToNBT());

        NBTTagList entities = new NBTTagList();
        entityCompounds.forEach(entities::appendTag);
        tagCompound.setTag("Entities", entities);

        NBTTagList tileEntitites = new NBTTagList();
        tileEntityCompounds.forEach(entities::appendTag);
        tagCompound.setTag("TileEntities", tileEntitites);
    }

    private int getBlockID(Block block)
    {
        return Block.REGISTRY.getIDForObject(block);
    }

    public static class UnsupportedSchematicFormatException extends Exception
    {
        public final String format;

        public UnsupportedSchematicFormatException(Throwable cause, String format)
        {
            super("Unsupported Schematic Format: " + format, cause);
            this.format = format;
        }

        public UnsupportedSchematicFormatException(String format)
        {
            this.format = format;
        }
    }
}
