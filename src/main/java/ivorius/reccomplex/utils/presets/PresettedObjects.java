/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.utils.presets;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import ivorius.reccomplex.json.JsonUtils;

import java.lang.reflect.Type;

/**
 * Created by lukas on 08.06.16.
 */
public class PresettedObjects
{
    public static <T> boolean read(JsonObject jsonObject, Gson gson, PresettedObject<T> object, String presetKey, String objectKey, Type type)
    {
        if (jsonObject.has(presetKey) && object.setPreset(JsonUtils.getString(jsonObject, presetKey)))
            return true;

        if (!jsonObject.has(objectKey))
        {
            object.setToDefault();
            return false;
        }

        object.setContents(gson.fromJson(jsonObject.get(objectKey), type));

        return true;
    }

    public static <T> void write(JsonObject jsonObject, Gson gson, PresettedObject<T> object, String presetKey, String objectKey)
    {
        if (object.getPreset() != null)
            jsonObject.addProperty(presetKey, object.getPreset());
        jsonObject.add(objectKey, gson.toJsonTree(object.getContents()));
    }
}
