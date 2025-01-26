/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.maze.rules;

import ivorius.ivtoolkit.maze.components.*;

import javax.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * Created by lukas on 16.04.15.
 */
public class LimitAABBStrategy<C> implements MazePredicate<C>, Predicate<MazeRoom>
{
    @Nonnull
    private int[] roomNumbers;

    public LimitAABBStrategy(@Nonnull int[] roomNumbers)
    {
        this.roomNumbers = roomNumbers;
    }

    @Override
    public boolean canPlace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<?, C> component)
    {
        return component.rooms().stream().allMatch(this);
    }

    @Override
    public void willPlace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<?, C> component)
    {

    }

    @Override
    public void didPlace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<?, C> component)
    {

    }

    @Override
    public void willUnplace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<?, C> component)
    {

    }

    @Override
    public void didUnplace(MorphingMazeComponent<C> maze, ShiftedMazeComponent<?, C> component)
    {

    }

    @Override
    public boolean isDirtyConnection(MazeRoom dest, MazeRoom source, C c)
    {
        return test(dest);
    }

    @Override
    public boolean test(MazeRoom input)
    {
        for (int i = 0; i < input.getDimensions(); i++)
            if (input.getCoordinate(i) < 0 || input.getCoordinate(i) >= roomNumbers[i])
                return false;
        return true;
    }
}
