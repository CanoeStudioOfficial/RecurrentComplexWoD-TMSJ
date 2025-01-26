/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.utils;

import net.minecraft.util.ResourceLocation;
import ivorius.ivtoolkit.tools.MCRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

/**
 * Created by lukas on 04.09.15.
 */
public class MCRegistryRemapping implements MCRegistry
{
    protected MCRegistry parent;
    protected FMLRemapper remapper;

    public MCRegistryRemapping(MCRegistry parent, FMLRemapper remapper)
    {
        this.parent = parent;
        this.remapper = remapper;
    }

    @Override
    public Item itemFromID(ResourceLocation itemID)
    {
        return parent.itemFromID(remapper.mapItem(itemID));
    }

    @Override
    public ResourceLocation idFromItem(Item item)
    {
        return parent.idFromItem(item);
    }

    @Override
    public void modifyItemStackCompound(NBTTagCompound compound, ResourceLocation itemID)
    {
        ResourceLocation mapped = remapper.remapItem(itemID);
        if (mapped != null)
        {
            itemID = mapped;
            compound.setString("id", mapped.toString());
        }

        parent.modifyItemStackCompound(compound, itemID);
    }

    @Override
    public Block blockFromID(ResourceLocation blockID)
    {
        return parent.blockFromID(remapper.mapBlock(blockID));
    }

    @Override
    public ResourceLocation idFromBlock(Block block)
    {
        return parent.idFromBlock(block);
    }

    @Override
    public TileEntity loadTileEntity(World world, NBTTagCompound compound)
    {
        ResourceLocation remap = remapper.remapTileEntity(new ResourceLocation(compound.getString("id")));

        if (remap != null)
        {
            NBTTagCompound copy = compound.copy();
            copy.setString("id", remap.toString());
            return parent.loadTileEntity(world, copy);
        }
        else
            return parent.loadTileEntity(world, compound);
    }
}
