/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.utils.expression;

import com.google.common.primitives.Ints;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.files.saving.FileSaver;
import ivorius.reccomplex.utils.algebra.BoolFunctionExpressionCache;
import ivorius.reccomplex.utils.algebra.RCBoolAlgebra;
import ivorius.reccomplex.utils.algebra.SupplierCache;
import ivorius.reccomplex.world.gen.feature.structure.Environment;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.biome.Biome;

import java.util.Objects;
import java.util.function.Function;

/**
 * Created by lukas on 07.09.16.
 */
public class EnvironmentExpression extends BoolFunctionExpressionCache<Environment, Object>
{
    public static final String BIOME_PREFIX = "biome.";
    public static final String DIMENSION_PREFIX = "dimension.";
    public static final String DEPENDENCY_PREFIX = "dependency.";
    public static final String VILLAGE_TYPE_PREFIX = "villagetype=";
    public static final String GENERATION_INFO_PREFIX = "generation.";
    public static final String VARIABLE_PREFIX = "variable.";

    public EnvironmentExpression()
    {
        super(RCBoolAlgebra.algebra(), true, TextFormatting.GREEN + "Always");

        addType(new BiomeVariableType(BIOME_PREFIX, ""));
        addTypes(new DimensionVariableType(DIMENSION_PREFIX, ""), t -> t.alias("dim.", ""));
        addTypes(new DependencyVariableType(DEPENDENCY_PREFIX, ""), t -> t.alias("dep.", ""));
        addTypes(new VillageTypeType(VILLAGE_TYPE_PREFIX, ""), t -> t.alias("vtype.", ""));
        addTypes(new GenerationType(GENERATION_INFO_PREFIX, ""), t -> t.alias("gen.", ""));
        addTypes(new VariableDomainType(VARIABLE_PREFIX, ""), t -> t.alias("var.", ""));
    }

    public static class BiomeVariableType extends DelegatingVariableType<Boolean, Environment, Object, Biome, Object, BiomeExpression>
    {
        public BiomeVariableType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Biome convertEvaluateArgument(String var, Environment argument)
        {
            return argument.biome;
        }

        @Override
        public BiomeExpression createCache()
        {
            return new BiomeExpression();
        }
    }

    public static class DimensionVariableType extends DelegatingVariableType<Boolean, Environment, Object, WorldProvider, Object, DimensionExpression>
    {
        public DimensionVariableType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public WorldProvider convertEvaluateArgument(String var, Environment argument)
        {
            return argument.world.provider;
        }

        @Override
        public DimensionExpression createCache()
        {
            return new DimensionExpression();
        }
    }

    public static class DependencyVariableType extends DelegatingVariableType<Boolean, Environment, Object, FileSaver, FileSaver, DependencyExpression>
    {
        public DependencyVariableType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public FileSaver convertArgument(String var, Environment environment)
        {
            return RecurrentComplex.saver;
        }

        @Override
        public DependencyExpression createCache()
        {
            return new DependencyExpression();
        }
    }

    protected static class VillageTypeType extends VariableType<Boolean, Environment, Object>
    {
        public VillageTypeType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Function<SupplierCache<Environment>, Boolean> parse(String var)
        {
            Integer villageType = parseVillageType(var);
            return environment -> Objects.equals(villageType, environment.get().villageType);
        }

        public Integer parseVillageType(String var)
        {
            Integer integer = Ints.tryParse(var);
            return integer != null && integer >= 0 && integer < 4 ? integer : null;
        }

        @Override
        public Validity validity(final String var, final Object args)
        {
            return parseVillageType(var) != null ? Validity.KNOWN : Validity.ERROR;
        }
    }

    protected static class GenerationType extends DelegatingVariableType<Boolean, Environment, Object, ivorius.reccomplex.world.gen.feature.structure.generic.generation.GenerationType, Object, GenerationTypeExpression>
    {
        public GenerationType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public ivorius.reccomplex.world.gen.feature.structure.generic.generation.GenerationType convertEvaluateArgument(String var, Environment environment)
        {
            return environment.generationType;
        }

        @Override
        public GenerationTypeExpression createCache()
        {
            return new GenerationTypeExpression();
        }
    }

    protected static class VariableDomainType extends VariableType<Boolean, Environment, Object>
    {
        public VariableDomainType(String prefix, String suffix)
        {
            super(prefix, suffix);
        }

        @Override
        public Function<SupplierCache<Environment>, Boolean> parse(String var)
        {
            return environment -> environment.get().variables.get(var);
        }

        @Override
        public Validity validity(final String var, final Object args)
        {
            return Validity.KNOWN;
        }
    }
}
