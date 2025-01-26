/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure;

import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.StructurePlaceContext;

/**
 * Created by lukas on 10.04.15.
 */
public interface Placer
{
    int DONT_GENERATE = -1;

    int place(StructurePlaceContext context, IvBlockCollection blockCollection);
}
