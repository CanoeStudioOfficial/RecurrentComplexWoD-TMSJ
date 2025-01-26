/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.storage.loot;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import ivorius.reccomplex.json.JsonUtils;

import java.lang.reflect.Type;

/**
 * Created by lukas on 25.05.14.
 */
public class WeightedRandomChestContentSerializer implements JsonSerializer<WeightedRandomChestContent>, JsonDeserializer<WeightedRandomChestContent>
{
    @Override
    public WeightedRandomChestContent deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
    {
        JsonObject jsonObject = JsonUtils.asJsonObject(json, "weightedRandomChestContent");

        int weight = JsonUtils.getInt(jsonObject, "weight");
        int genMin = JsonUtils.getInt(jsonObject, "genMin");
        int genMax = JsonUtils.getInt(jsonObject, "genMax");
        ItemStack stack = context.deserialize(jsonObject.get("item"), ItemStack.class);

        return new WeightedRandomChestContent(stack, genMin, genMax, weight);
    }

    @Override
    public JsonElement serialize(WeightedRandomChestContent src, Type typeOfSrc, JsonSerializationContext context)
    {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("weight", src.itemWeight);
        jsonObject.addProperty("genMin", src.minStackSize);
        jsonObject.addProperty("genMax", src.maxStackSize);
        jsonObject.add("item", context.serialize(src.theItemId, ItemStack.class));

        return jsonObject;
    }

}
