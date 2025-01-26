/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.world.gen.feature.structure.generic.placement;

import com.google.common.math.DoubleMath;
import com.google.gson.*;
import ivorius.ivtoolkit.blocks.BlockAreas;
import ivorius.ivtoolkit.blocks.IvBlockCollection;
import ivorius.ivtoolkit.gui.IntegerRange;
import ivorius.ivtoolkit.util.LineSelection;
import ivorius.ivtoolkit.world.WorldCache;
import ivorius.ivtoolkit.world.chunk.gen.StructureBoundingBoxes;
import ivorius.reccomplex.RecurrentComplex;
import ivorius.reccomplex.gui.editstructure.placer.TableDataSourceFactorMatch;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.datasource.TableDataSource;
import ivorius.reccomplex.json.JsonUtils;
import ivorius.reccomplex.utils.IntegerRanges;
import ivorius.reccomplex.utils.algebra.ExpressionCache;
import ivorius.reccomplex.utils.expression.BlockExpression;
import ivorius.reccomplex.utils.expression.PositionedBlockExpression;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by lukas on 18.09.16.
 */
public class FactorMatch extends GenericPlacer.Factor
{
    public BlockExpression sourceMatcher;
    public PositionedBlockExpression destMatcher;

    public float requiredConformity;

    public FactorMatch()
    {
        this(1, "", "", .5f);
    }

    public FactorMatch(float priority, String sourceExpression, String destExpression, float requiredConformity)
    {
        super(priority);
        this.sourceMatcher = ExpressionCache.of(new BlockExpression(RecurrentComplex.specialRegistry), sourceExpression);
        this.destMatcher = ExpressionCache.of(new PositionedBlockExpression(RecurrentComplex.specialRegistry), destExpression);

        this.requiredConformity = requiredConformity;
    }

    protected float weight(WorldCache cache, Set<? extends BlockPos> sources, float needed)
    {
        int failChances = (int) (sources.size() * (1f - needed));
        int matched = 0;

        for (BlockPos pos : sources)
        {
            if (destMatcher.evaluate(() -> PositionedBlockExpression.Argument.at(cache, pos)))
                matched++;
            else if (--failChances < 0)
                break;  // Already lost
        }

        return failChances >= 0 ? (float) matched / sources.size() : 0;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public TableDataSource tableDataSource(TableNavigator navigator, TableDelegate delegate)
    {
        return new TableDataSourceFactorMatch(this, delegate, navigator);
    }

    @Override
    public List<Pair<LineSelection, Float>> consider(WorldCache cache, LineSelection considerable, @Nullable IvBlockCollection blockCollection, Set<BlockPos> surface, StructurePlaceContext context)
    {
        if (blockCollection == null)
            throw new IllegalArgumentException("Missing a block collection!");

        List<Pair<LineSelection, Float>> consideration = new ArrayList<>();

        int[] size = StructureBoundingBoxes.size(context.boundingBox);
        BlockPos lowerCoord = StructureBoundingBoxes.min(context.boundingBox);
        Set<BlockPos.MutableBlockPos> sources = BlockAreas.streamMutablePositions(blockCollection.area())
                .filter(p -> sourceMatcher.evaluate(() -> blockCollection.getBlockState(p)))
                .map(p -> new BlockPos.MutableBlockPos(context.transform.apply(p, size).add(lowerCoord.getX(), 0, lowerCoord.getZ())))
                .collect(Collectors.toSet());

        for (IntegerRange range : (Iterable<IntegerRange>) considerable.streamSections(null, true)::iterator)
        {
            Float curConformity = null;
            int lastY = range.getMax();
            int end = range.getMin();

            for (int y = lastY; y >= end; y--)
            {
                int finalY = y;
                sources.forEach(p -> p.move(EnumFacing.UP, finalY));

                float conformity = weight(cache, sources, requiredConformity);

                sources.forEach(p -> p.move(EnumFacing.DOWN, finalY));

                if (curConformity == null)
                {
                    curConformity = conformity;
                    lastY = y;
                }
                else if (!DoubleMath.fuzzyEquals(conformity, curConformity, 0.01))
                {
                    consideration.add(Pair.of(LineSelection.fromRange(IntegerRanges.from(lastY, y + 1), true), weight(curConformity)));

                    curConformity = conformity;
                    lastY = y;
                }
            }

            if (curConformity != null)
                consideration.add(Pair.of(LineSelection.fromRange(IntegerRanges.from(lastY, end), true), weight(curConformity)));
        }

        return consideration;
    }

    public static class Serializer implements JsonSerializer<FactorMatch>, JsonDeserializer<FactorMatch>
    {
        @Override
        public FactorMatch deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException
        {
            JsonObject jsonObject = JsonUtils.asJsonObject(json, "factorMatch");

            float priority = JsonUtils.getFloat(jsonObject, "priority", 1);

            String sourceExpression = JsonUtils.getString(jsonObject, "sourceExpression", "");
            String destExpression = JsonUtils.getString(jsonObject, "destExpression", "");

            float requiredConformity = JsonUtils.getFloat(jsonObject, "requiredConformity", 0);

            return new FactorMatch(priority, sourceExpression, destExpression, requiredConformity);
        }

        @Override
        public JsonElement serialize(FactorMatch src, Type typeOfSrc, JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();

            jsonObject.addProperty("priority", src.priority);

            jsonObject.addProperty("sourceExpression", src.sourceMatcher.getExpression());
            jsonObject.addProperty("destExpression", src.destMatcher.getExpression());

            jsonObject.addProperty("requiredConformity", src.requiredConformity);

            return jsonObject;
        }
    }
}
