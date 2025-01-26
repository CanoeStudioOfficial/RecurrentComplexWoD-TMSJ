/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.loot;

import io.netty.buffer.ByteBuf;
import ivorius.ivtoolkit.network.PacketGuiAction;
import ivorius.reccomplex.gui.SlotDynamicIndex;
import ivorius.reccomplex.world.storage.loot.GenericLootTable.Component;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lukas on 26.05.14.
 */

public class ContainerEditLootTableItems extends Container implements PacketGuiAction.ActionHandler
{
    public static final int ITEM_ROWS = 4;
    public static final int ITEM_COLUMNS = 1;
    public static final int ITEMS_PER_PAGE = ITEM_ROWS * ITEM_COLUMNS;
    public static final int SEGMENT_WIDTH = 288;

    private final String key;
    private final Component component;

    public InventoryGenericLootTable_Single inventory;

    private List<SlotDynamicIndex> scrollableSlots = new ArrayList<>();

    public ContainerEditLootTableItems(EntityPlayer player, String key, Component component)
    {
        inventory = new InventoryGenericLootTable_Single(component.items);

        this.key = key;
        this.component = component;

        InventoryPlayer inventoryplayer = player.inventory;

        for (int col = 0; col < ITEM_COLUMNS; ++col)
        {
            for (int row = 0; row < ITEM_ROWS; ++row)
            {
                int index = col * ITEM_ROWS + row;
                SlotDynamicIndex slotLeft = new SlotDynamicIndex(inventory, index, col * SEGMENT_WIDTH, 20 + row * 18);

                this.addSlotToContainer(slotLeft);
                scrollableSlots.add(slotLeft);
            }
        }

        int basePlayerY = 20 + ITEM_ROWS * 18 + 13;
        int basePlayerX = (ITEM_COLUMNS * SEGMENT_WIDTH - 9 * 18) / 2;

        for (int row = 0; row < 3; ++row)
        {
            for (int col = 0; col < 9; ++col)
            {
                int index = col + row * 9 + 9;
                this.addSlotToContainer(new Slot(inventoryplayer, index, basePlayerX + col * 18, basePlayerY + row * 18));
            }
        }

        for (int col = 0; col < 9; ++col)
        {
            int index = col;
            this.addSlotToContainer(new Slot(inventoryplayer, index, basePlayerX + col * 18, basePlayerY + 3 * 18 + 4));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer var1)
    {
        return true;
    }

    public void scrollTo(int colShift)
    {
//        List<WeightedRandomChestContent> items = lootTable.weightedRandomChestContents;

        for (int col = 0; col < ITEM_COLUMNS; ++col)
        {
            for (int row = 0; row < ITEM_ROWS; ++row)
            {
                int index = row + (col + colShift) * ITEM_ROWS;
                int scrollableSlotsIndex = row + col * ITEM_ROWS;

                scrollableSlots.get(scrollableSlotsIndex).slotIndex = index;
//                if (index >= 0 && index < items.size())
//                {
////                    inventory.setInventorySlotContents(col + row * 9, items.get(index).theItemId);
//                }
//                else
//                {
////                    inventory.setInventorySlotContents(col + row * 9, ItemStack.EMPTY);
//                }
            }
        }
    }

    @Override
    public void handleAction(String context, ByteBuf buffer)
    {
        if ("igSelectCol".equals(context))
        {
            scrollTo(buffer.readInt());
        }
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex)
    {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack())
        {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            int generatorIndexMax = ITEM_COLUMNS * ITEM_ROWS;
            if (slotIndex < generatorIndexMax)
            {
                if (!this.mergeItemStack(itemstack1, generatorIndexMax, generatorIndexMax + 36, true))
                {
                    return ItemStack.EMPTY;
                }
            }
            else if (!this.mergeItemStack(itemstack1, 0, generatorIndexMax, false))
            {
                return ItemStack.EMPTY;
            }

            if (itemstack1.getCount() == 0)
            {
                slot.putStack(ItemStack.EMPTY);
            }
            else
            {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount())
            {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }
}
