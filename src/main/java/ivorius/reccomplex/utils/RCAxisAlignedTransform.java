/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.utils;

import ivorius.ivtoolkit.blocks.BlockArea;
import ivorius.ivtoolkit.math.AxisAlignedTransform2D;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;

/**
 * Created by lukas on 22.09.16.
 */
public class RCAxisAlignedTransform
{
    public static BlockPos.MutableBlockPos apply(BlockPos.MutableBlockPos position, int[] size, AxisAlignedTransform2D transform)
    {
        return apply(position, position, size, transform);
    }

    public static BlockPos.MutableBlockPos apply(BlockPos position, BlockPos.MutableBlockPos onPosition, int[] size, AxisAlignedTransform2D transform)
    {
        boolean mirrorX = transform.isMirrorX();
        int rotation = transform.getRotation();

        int positionX = mirrorX ? size[0] - 1 - position.getX() : position.getX();

        switch (rotation)
        {
            case 0:
                return onPosition.setPos(positionX, position.getY(), position.getZ());
            case 1:
                return onPosition.setPos(size[2] - 1 - position.getZ(), position.getY(), positionX);
            case 2:
                return onPosition.setPos(size[0] - 1 - positionX, position.getY(), size[2] - 1 - position.getZ());
            case 3:
                return onPosition.setPos(position.getZ(), position.getY(), size[0] - 1 - positionX);
            default:
                throw new InternalError();
        }
    }

    public static BlockArea apply(AxisAlignedTransform2D transform, BlockArea area, int[] size, int centerCorrection)
    {
        return apply(transform, area, new BlockArea(BlockPos.ORIGIN, BlockPos.ORIGIN), size, centerCorrection);
    }

    public static BlockArea apply(AxisAlignedTransform2D transform, BlockArea area, BlockArea on, int[] size, int centerCorrection)
    {
        on.setPoint1(transform.apply(area.getPoint1(), size, centerCorrection));
        on.setPoint2(transform.apply(area.getPoint2(), size, centerCorrection));
        return on;
    }

    public static AxisAlignedTransform2D invert(AxisAlignedTransform2D transform2D)
    {
        // Black Magic
        return AxisAlignedTransform2D.from(transform2D.isMirrorX() ? transform2D.getRotation() : -transform2D.getRotation(),
                transform2D.isMirrorX());
    }

    public static int[] applySize(AxisAlignedTransform2D transform, int[] size)
    {
        if (transform.getRotation() % 2 == 1)
        {
            size = size.clone();

            int cache = size[0];
            size[0] = size[2];
            size[2] = cache;
            return size;
        }
        return size;
    }

    public static AxisAlignedTransform2D read(NBTTagCompound compound, String rotation, String mirrorX)
    {
        return AxisAlignedTransform2D.from(compound.getInteger(rotation), compound.getBoolean(mirrorX));
    }

    public static void write(NBTTagCompound compound, AxisAlignedTransform2D transform, String rotation, String mirrorX)
    {
        compound.setInteger(rotation, transform.getRotation());
        compound.setBoolean(mirrorX, transform.isMirrorX());
    }
}
