/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.storage.loot;

import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldServer;

import java.util.Random;

/**
 * Created by lukas on 25.05.14.
 */
public interface LootTable
{
    ItemStack getRandomItemStack(WorldServer server, Random random);

    String getDescriptor();
}
