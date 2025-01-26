/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.loot;

import ivorius.reccomplex.gui.InventoryWatcher;
import ivorius.reccomplex.world.storage.loot.WeightedRandomChestContent;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by lukas on 27.05.14.
 */

@SideOnly(Side.CLIENT)
public class InventoryGenericLootGen implements IInventory
{
    public List<WeightedRandomChestContent> chestContents;
    private List<ItemStack> cachedItemStacks = new ArrayList<>();

    private List<InventoryWatcher> watchers = new ArrayList<>();

    public InventoryGenericLootGen(List<WeightedRandomChestContent> chestContents)
    {
        this.chestContents = chestContents;

        buildCachedStacks();
    }

    public void addWatcher(InventoryWatcher watcher)
    {
        watchers.add(watcher);
    }

    public void removeWatcher(InventoryWatcher watcher)
    {
        watchers.remove(watcher);
    }

    public List<InventoryWatcher> watchers()
    {
        return Collections.unmodifiableList(watchers);
    }

    @Override
    public int getSizeInventory()
    {
        return cachedItemStacks.size();
    }

    @Override
    public boolean isEmpty()
    {
        for (ItemStack itemstack : this.cachedItemStacks)
        {
            if (!itemstack.isEmpty())
            {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStackInSlot(int var1)
    {
        return var1 < cachedItemStacks.size() ? cachedItemStacks.get(var1) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int var1, int var2)
    {
        int stackIndex = var1 / 2;

        if (stackIndex < chestContents.size())
        {
            WeightedRandomChestContent chestContent = chestContents.get(stackIndex);

            if (var1 % 2 == 0)
            {
                chestContent.minStackSize -= var2;
            }
            else
            {
                chestContent.maxStackSize -= var2;
            }

            validateMinMax(chestContent);

//            if (chestContent.minStackSize <= 0 || chestContent.maxStackSize <= 0)
//                chestContents.remove(stackIndex);

            ItemStack returnStack = cachedItemStacks.get(var1).splitStack(var2);
            markDirtyFromLootGenerator();
            return returnStack;
        }
        else
        {
            return null;
        }
    }

    @Override
    public ItemStack removeStackFromSlot(int var1)
    {
        return ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int var1, ItemStack var2)
    {
        int stackIndex = var1 / 2;

        if (stackIndex < chestContents.size())
        {
            if (!var2.isEmpty())
            {
                WeightedRandomChestContent chestContent = chestContents.get(stackIndex);
                chestContent.theItemId = var2;

                if (var1 % 2 == 0)
                {
                    chestContent.minStackSize = var2.getCount();
                }
                else
                {
                    chestContent.maxStackSize = var2.getCount();
                }

                validateMinMax(chestContent);
            }
            else
            {
                chestContents.remove(stackIndex);
            }
        }
        else
        {
            if (!var2.isEmpty())
            {
                int min = var1 % 2 == 0 ? var2.getCount() : 1;
                int max = var1 % 2 == 1 ? var2.getCount() : var2.getMaxStackSize();

                WeightedRandomChestContent weightedRandomChestContent = new WeightedRandomChestContent(var2, min, max, 100);
                chestContents.add(weightedRandomChestContent);
            }
        }

        markDirtyFromLootGenerator();
    }

    private static void validateMinMax(WeightedRandomChestContent chestContent)
    {
        if (chestContent.maxStackSize < chestContent.minStackSize)
        {
            int tmp = chestContent.maxStackSize;
            chestContent.maxStackSize = chestContent.minStackSize;
            chestContent.minStackSize = tmp;
        }
    }

    @Override
    public int getInventoryStackLimit()
    {
        return 64;
    }

    @Override
    public void markDirty()
    {
        calculateMinMax();
        buildCachedStacks();

        for (InventoryWatcher watcher : watchers)
        {
            watcher.inventoryChanged(this);
        }
    }

    public void markDirtyFromLootGenerator()
    {
        buildCachedStacks();

        for (InventoryWatcher watcher : watchers)
        {
            watcher.inventoryChanged(this);
        }
    }

    private void calculateMinMax()
    {
        for (int i = 0; i < cachedItemStacks.size(); i++)
        {
            ItemStack stack = cachedItemStacks.get(i);
            int stackIndex = i / 2;

            if (stackIndex < chestContents.size())
            {
                WeightedRandomChestContent chestContent = chestContents.get(stackIndex);

                if (i % 2 == 0)
                {
                    chestContent.minStackSize = stack.getCount();
                }
                else
                {
                    chestContent.maxStackSize = stack.getCount();
                }
            }
        }

        chestContents.forEach(InventoryGenericLootGen::validateMinMax);
    }

    private void buildCachedStacks()
    {
        cachedItemStacks.clear();
        for (WeightedRandomChestContent chestContent : chestContents)
        {
            ItemStack stackLow = chestContent.theItemId.copy();
            stackLow.setCount(chestContent.minStackSize);
            ItemStack stackHigh = chestContent.theItemId.copy();
            stackHigh.setCount(chestContent.maxStackSize);

            cachedItemStacks.add(stackLow);
            cachedItemStacks.add(stackHigh);
        }
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player)
    {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player)
    {

    }

    @Override
    public void closeInventory(EntityPlayer player)
    {

    }

    @Override
    public boolean isItemValidForSlot(int var1, ItemStack var2)
    {
        return true;
    }

    @Override
    public int getField(int id)
    {
        return 0;
    }

    @Override
    public void setField(int id, int value)
    {

    }

    @Override
    public int getFieldCount()
    {
        return 0;
    }

    @Override
    public void clear()
    {

    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public boolean hasCustomName()
    {
        return false;
    }

    @Override
    public ITextComponent getDisplayName()
    {
        return null;
    }
}
