/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic;

import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.utils.algebra.ExpressionCache;
import ivorius.reccomplex.utils.expression.EnvironmentExpression;
import ivorius.reccomplex.world.gen.feature.structure.Environment;
import ivorius.reccomplex.world.gen.feature.structure.VariableDomain;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

/**
 * Created by lukas on 15.04.17.
 */
public class GenericVariableDomain
{
    @SerializedName("variables")
    protected final List<Variable> variables = new ArrayList<>();

    public List<Variable> variables()
    {
        return variables;
    }

    public void fill(VariableDomain domain, Environment environment, Random random)
    {
        for (Variable variable : variables)
        {
            if (environment.variables.isSet(variable.id))
            {
                domain.set(variable.id, environment.variables.get(variable.id));
                return;
            }

            if (domain.isSet(variable.id))
            {
                environment.variables.set(variable.id, domain.get(variable.id));
                return;
            }

            boolean value = random.nextFloat() < variable.chance
                    && variable.condition.test(environment);

            domain.set(variable.id, value);
            environment.variables.set(variable.id, value);
        }
    }

    public float chance(Environment environment)
    {
        // TODO Support for variables changing value? (environment needs to be the same at set time)
        float chance = 1f;
        for (Variable variable : variables)
            chance *= chance(variable, environment.variables.get(variable.id), environment);
        return chance;
    }

    public float chance(Variable variable, boolean value, Environment environment)
    {
        return variable.condition.test(environment) ? value ? variable.chance : 1f - variable.chance : (value ? 0 : 1);
    }

    public Stream<VariableDomain> omega(Environment environment, boolean logical)
    {
        Stream<VariableDomain> stream = Stream.of(environment.variables.copy());
        for (GenericVariableDomain.Variable variable : this.variables)
        {
            if ((!logical || variable.affectsLogic) && !environment.variables.isSet(variable.id))
            {
                stream = stream.flatMap(d -> {
                    VariableDomain cached = environment.variables;
                    environment.variables = d;

                    boolean top = variable.chance > 0 && variable.condition.test(environment);
                    boolean bottom = variable.chance < 1;

                    environment.variables = cached;

                    if (top && bottom)
                        return d.split(variable.id);
                    return Stream.of(d.set(variable.id, top));
                });
            }
        }
        return stream;
    }

    public static class Variable
    {
        public String id = "";

        public EnvironmentExpression condition = ExpressionCache.of(new EnvironmentExpression(), "");

        public float chance = 0.5f;

        public boolean affectsLogic = false;

        public Variable copy()
        {
            Variable var = new Variable();

            var.id = id;
            var.condition.setExpression(condition.getExpression());
            var.chance = chance;
            var.affectsLogic = affectsLogic;

            return var;
        }

        public static class Serializer implements JsonSerializer<Variable>, JsonDeserializer<Variable>
        {
            @Override
            public Variable deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
            {
                JsonObject jsonObject = JsonUtils.asJsonObject(json, "Generic Variable");

                Variable variable = new Variable();

                variable.id = JsonUtils.getString(jsonObject, "id");
                variable.condition.setExpression(JsonUtils.getString(jsonObject, "condition", ""));
                variable.chance = JsonUtils.getFloat(jsonObject, "chance");
                variable.affectsLogic = JsonUtils.getBoolean(jsonObject, "affectsLogic");

                return variable;
            }

            @Override
            public JsonElement serialize(Variable src, Type typeOfSrc, JsonSerializationContext context)
            {
                JsonObject jsonObject = new JsonObject();

                jsonObject.addProperty("id", src.id);
                jsonObject.addProperty("condition", src.condition.getExpression());
                jsonObject.addProperty("chance", src.chance);
                jsonObject.addProperty("affectsLogic", src.affectsLogic);

                return jsonObject;
            }
        }
    }
}
