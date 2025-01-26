/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import ivorius.ivtoolkit.blocks.BlockPositions;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import ivorius.ivtoolkit.tools.NBTCompoundObject;
import ivorius.ivtoolkit.tools.NBTCompoundObjects;
import ivorius.ivtoolkit.tools.NBTTagLists;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.utils.RCAxisAlignedTransform;
import ivorius.reccomplex.utils.RCStructureBoundingBoxes;
import ivorius.reccomplex.world.gen.feature.structure.Structure;
import ivorius.reccomplex.world.gen.feature.structure.StructureRegistry;
import ivorius.reccomplex.world.gen.feature.structure.Structures;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by lukas on 01.03.15.
 */
public class WorldStructureGenerationData extends WorldSavedData
{
    private static final String IDENTIFIER = RecurrentComplex.MOD_ID + "-structuredata";

    protected final Set<ChunkPos> checkedChunks = new HashSet<>();
    protected final Set<ChunkPos> checkedChunksFinal = new HashSet<>();

    protected final Map<UUID, Entry> entryMap = new HashMap<>();
    protected final SetMultimap<ChunkPos, Entry> chunkMap = HashMultimap.create();

    protected final SetMultimap<String, StructureEntry> instanceMap = HashMultimap.create();

    public WorldStructureGenerationData(String id)
    {
        super(id);
    }

    public WorldStructureGenerationData()
    {
        this(IDENTIFIER);
    }

    public static WorldStructureGenerationData get(World world)
    {
        WorldStructureGenerationData data = (WorldStructureGenerationData) world.getPerWorldStorage().getOrLoadData(WorldStructureGenerationData.class, IDENTIFIER);
        if (data == null)
        {
            data = new WorldStructureGenerationData();
            world.getPerWorldStorage().setData(data.mapName, data);
        }
        return data;
    }

    public Stream<StructureEntry> structureEntriesIn(ChunkPos coords)
    {
        return entriesAt(coords)
                .filter(StructureEntry.class::isInstance).map(StructureEntry.class::cast);
    }

    public Stream<Entry> entriesAt(ChunkPos coords)
    {
        return chunkMap.get(coords).stream();
    }

    public Stream<Entry> entriesAt(final BlockPos coords)
    {
        // Add 8 for both since the chunk map is rasterized for decoration where every chunk is +8
        return entriesAt(new ChunkPos((coords.getX() - 8) >> 4, (coords.getZ() - 8) >> 4))
                .filter(input ->
                {
                    StructureBoundingBox bb = input.getBoundingBox();
                    return bb != null && bb.isVecInside(coords);
                });
    }

    public Stream<Entry> entriesAt(final StructureBoundingBox boundingBox)
    {
        return RCStructureBoundingBoxes.rasterize(boundingBox, true).stream().flatMap(chunkPos -> entriesAt(chunkPos).filter(input ->
        {
            StructureBoundingBox bb = input.getBoundingBox();
            return bb != null && bb.intersectsWith(boundingBox);
        }));
    }

    public Set<ChunkPos> addEntry(Entry entry)
    {
        entryMap.put(entry.getUuid(), entry);

        Set<ChunkPos> rasterized = entry.rasterize();
        for (ChunkPos coords : rasterized)
            chunkMap.put(coords, entry);

        if (entry instanceof StructureEntry)
            instanceMap.put(((StructureEntry) entry).getStructureID(), (StructureEntry) entry);

        markDirty();

        return Sets.intersection(checkedChunks, rasterized);
    }

    public Entry getEntry(UUID id)
    {
        return entryMap.get(id);
    }

    public Entry removeEntry(UUID id)
    {
        Entry entry = entryMap.remove(id);
        chunkMap.values().removeIf(e -> e.uuid.equals(id));
        instanceMap.values().removeIf(e -> e.uuid.equals(id));

        if (entry != null)
            markDirty();
        return entry;
    }

    public Set<StructureEntry> getEntriesByID(String id)
    {
        return instanceMap.get(id);
    }

    public Stream<ChunkPos> checkAllChunks(Stream<ChunkPos> chunks)
    {
        return chunks.filter(this::checkChunk);
    }

    public boolean checkChunk(ChunkPos coords)
    {
        boolean added = checkedChunks.add(coords);
        if (added)
            markDirty();
        return added;
    }

    //
    public boolean checkChunkFinal(ChunkPos coords)
    {
        boolean added = checkedChunksFinal.add(coords);
        if (added)
            markDirty();
        return added;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound)
    {
        entryMap.clear();
        chunkMap.clear();
        instanceMap.clear();
        NBTCompoundObjects.readListFrom(compound, "entries", StructureEntry::new).forEach(this::addEntry);
        NBTCompoundObjects.readListFrom(compound, "customEntries", CustomEntry::new).forEach(this::addEntry);

        checkedChunks.clear();
        NBTTagLists.intArraysFrom(compound, "checkedChunks").forEach(ints -> checkedChunks.add(new ChunkPos(ints[0], ints[1])));
        checkedChunksFinal.clear();
        NBTTagLists.intArraysFrom(compound, "checkedChunksFinal").forEach(ints -> checkedChunksFinal.add(new ChunkPos(ints[0], ints[1])));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound)
    {
        NBTCompoundObjects.writeListTo(compound, "entries", entryMap.values().stream().filter(e -> e instanceof StructureEntry).collect(Collectors.toList()));
        NBTCompoundObjects.writeListTo(compound, "customEntries", entryMap.values().stream().filter(e -> e instanceof CustomEntry).collect(Collectors.toList()));

        NBTTagLists.writeIntArraysTo(compound, "checkedChunks", checkedChunks.stream().map(c -> new int[]{c.x, c.z}).collect(Collectors.toList()));
        NBTTagLists.writeIntArraysTo(compound, "checkedChunksFinal", checkedChunksFinal.stream().map(c -> new int[]{c.x, c.z}).collect(Collectors.toList()));

        return compound;
    }

    public static abstract class Entry implements NBTCompoundObject
    {
        @Nonnull
        protected UUID uuid = UUID.randomUUID();
        protected StructureBoundingBox boundingBox = new StructureBoundingBox();
        protected boolean blocking = true;

        public Entry()
        {
        }

        public Entry(@Nonnull UUID uuid, StructureBoundingBox boundingBox)
        {
            this.uuid = uuid;
            this.boundingBox = boundingBox;
        }

        @Nonnull
        public UUID getUuid()
        {
            return uuid;
        }

        public void setUuid(@Nonnull UUID uuid)
        {
            this.uuid = uuid;
        }

        public StructureBoundingBox getBoundingBox()
        {
            return boundingBox;
        }

        public void setBoundingBox(StructureBoundingBox boundingBox)
        {
            this.boundingBox = boundingBox;
        }

        public boolean blocking()
        {
            return blocking;
        }

        public void setBlocking(boolean blocking)
        {
            this.blocking = blocking;
        }

        public Set<ChunkPos> rasterize()
        {
            return RCStructureBoundingBoxes.rasterize(getBoundingBox(), true);
        }

        public abstract String description();

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            StructureEntry entry = (StructureEntry) o;

            return uuid.equals(entry.uuid);
        }

        @Override
        public int hashCode()
        {
            return uuid.hashCode();
        }

        @Override
        public void readFromNBT(NBTTagCompound compound)
        {
            uuid = new UUID(compound.getLong("UUIDMS"), compound.getLong("UUIDLS"));

            boundingBox = new StructureBoundingBox(compound.getIntArray("boundingBox"));

            blocking = !compound.hasKey("blocking", Constants.NBT.TAG_BYTE) || compound.getBoolean("blocking");
        }

        @Override
        public void writeToNBT(NBTTagCompound compound)
        {
            compound.setLong("UUIDMS", uuid.getMostSignificantBits());
            compound.setLong("UUIDLS", uuid.getLeastSignificantBits());

            compound.setTag("boundingBox", boundingBox.toNBTTagIntArray());

            compound.setBoolean("blocking", blocking);
        }
    }

    public static class StructureEntry extends Entry
    {
        protected String structureID;
        protected String generationInfoID;

        protected long seed;

        protected AxisAlignedTransform2D transform;

        protected NBTBase instanceData;
        protected boolean firstTime = true;

        protected boolean preventComplementation = false;

        public StructureEntry()
        {
        }

        public StructureEntry(@Nonnull UUID uuid, StructureBoundingBox boundingBox, String structureID, String generationInfoID, AxisAlignedTransform2D transform, boolean preventComplementation)
        {
            super(uuid, boundingBox);
            this.structureID = structureID;
            this.generationInfoID = generationInfoID;
            this.transform = transform;
            this.preventComplementation = preventComplementation;
        }

        @Nonnull
        public static StructureEntry complete(String structureID, String generationInfoID, StructureBoundingBox boundingBox, AxisAlignedTransform2D transform, boolean hasBeenGenerated)
        {
            return new StructureEntry(UUID.randomUUID(), boundingBox, structureID, generationInfoID, transform, hasBeenGenerated);
        }

        public String getStructureID()
        {
            return structureID;
        }

        public AxisAlignedTransform2D getTransform()
        {
            return transform;
        }

        public long getSeed()
        {
            return seed;
        }

        public boolean preventComplementation()
        {
            return preventComplementation;
        }

        public void setPreventComplementation(boolean preventComplementation)
        {
            this.preventComplementation = preventComplementation;
        }

        public NBTBase getInstanceData()
        {
            return instanceData;
        }

        @Override
        public void readFromNBT(NBTTagCompound compound)
        {
            super.readFromNBT(compound);

            structureID = compound.getString("structureID");
            generationInfoID = compound.hasKey(generationInfoID, Constants.NBT.TAG_STRING) ? compound.getString("generationInfoID") : null;

            seed = compound.hasKey("seed") ? compound.getLong("seed")
                    : new Random().nextLong(); // Legacy

            transform = RCAxisAlignedTransform.read(compound, "rotation", "mirrorX");

            if (compound.hasKey("lowerCoord")) // legacy
            {
                BlockPos lowerCoord = BlockPositions.readFromNBT("lowerCoord", compound);
                Structure structure = StructureRegistry.INSTANCE.get(structureID);

                boundingBox = structure != null
                        ? Structures.boundingBox(lowerCoord, RCAxisAlignedTransform.applySize(transform, structure.size()))
                        : new StructureBoundingBox();
            }

            if (compound.hasKey("instanceData", Constants.NBT.TAG_COMPOUND))
                instanceData = compound.getCompoundTag("instanceData");
            firstTime = compound.getBoolean("firstTime");
            preventComplementation = compound.getBoolean("preventComplementation")
                    || compound.getBoolean("hasBeenGenerated"); // Legacy
        }

        @Override
        public void writeToNBT(NBTTagCompound compound)
        {
            super.writeToNBT(compound);

            compound.setString("structureID", structureID);
            if (generationInfoID != null) compound.setString("generationInfoID", generationInfoID);

            compound.setLong("seed", seed);

            RCAxisAlignedTransform.write(compound, transform, "rotation", "mirrorX");

            if (instanceData != null)
                compound.setTag("instanceData", instanceData);
            compound.setBoolean("firstTime", firstTime);
            compound.setBoolean("preventComplementation", preventComplementation);
        }

        @Override
        public String description()
        {
            return structureID;
        }
    }

    public static class CustomEntry extends Entry
    {
        public String name;

        public CustomEntry()
        {
        }

        public CustomEntry(@Nonnull UUID uuid, StructureBoundingBox boundingBox, String name)
        {
            super(uuid, boundingBox);
            this.name = name;
        }

        @Nonnull
        public static CustomEntry from(String name, StructureBoundingBox boundingBox)
        {
            return new CustomEntry(UUID.randomUUID(), boundingBox, name);
        }

        @Override
        public String description()
        {
            return name;
        }

        @Override
        public void writeToNBT(NBTTagCompound compound)
        {
            super.writeToNBT(compound);

            compound.setString("name", name);
        }

        @Override
        public void readFromNBT(NBTTagCompound compound)
        {
            super.readFromNBT(compound);

            name = compound.getString("name");
        }
    }
}
