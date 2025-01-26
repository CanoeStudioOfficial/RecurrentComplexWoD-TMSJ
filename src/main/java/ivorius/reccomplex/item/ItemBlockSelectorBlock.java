/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.item;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockSelectorBlock extends ItemBlockSelector
{
    public ItemBlockSelectorBlock()
    {
    }

    @Nullable
    @Override
    public BlockPos hoveredBlock(ItemStack stack, EntityLivingBase entity)
    {
        RayTraceResult rayTraceResult = entity.rayTrace(300, 0);
        return rayTraceResult != null ? rayTraceResult.getBlockPos() : null;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn)
    {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        tooltip.add("(Hold ctrl for secondary selection)");
    }
}
