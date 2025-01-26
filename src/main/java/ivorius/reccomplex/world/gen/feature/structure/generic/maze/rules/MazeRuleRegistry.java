/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.maze.rules;

import ivorius.reccomplex.nbt.NBTStringTypeRegistry;

/**
 * Created by lukas on 21.03.16.
 */
public class MazeRuleRegistry extends NBTStringTypeRegistry<MazeRule>
{
    public static final MazeRuleRegistry INSTANCE = new MazeRuleRegistry("rule", "type");

    public MazeRuleRegistry(String objectKey, String typeKey)
    {
        super(objectKey, typeKey);
    }

    public MazeRuleRegistry()
    {
    }
}
