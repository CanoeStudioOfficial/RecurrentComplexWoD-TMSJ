/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature.selector;

import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;

/**
 * Created by lukas on 23.09.16.
 */
public interface EnvironmentalSelection<C>
{
    double getGenerationWeight(WorldProvider provider, Biome biome);

    C generationCategory();
}
