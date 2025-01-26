/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.ivtoolkit.gui.FloatRange;
import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.TableDirections;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.*;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.utils.scale.Scales;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerRuins;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Created by lukas on 05.06.14.
 */

@SideOnly(Side.CLIENT)
public class TableDataSourceBTRuins extends TableDataSourceSegmented
{
    private TransformerRuins transformer;

    public TableDataSourceBTRuins(TransformerRuins transformer, TableNavigator navigator, TableDelegate delegate)
    {
        this.transformer = transformer;

        addSegment(0, new TableDataSourceTransformer(transformer, delegate, navigator));

        addSegment(1, () -> {
            return new TitledCell(new TableCellTitle("decayTitle", IvTranslations.get("reccomplex.transformer.ruins.decay.title")));
        }, () -> {
            TableCellFloatRange cell = new TableCellFloatRange("decay", new FloatRange(transformer.minDecay, transformer.maxDecay), 0.0f, 1.0f, "%.4f");
            cell.setScale(Scales.pow(5));
            cell.addListener(val ->
            {
                transformer.minDecay = val.getMin();
                transformer.maxDecay = val.getMax();
            });
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.base"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.base.tooltip"));
        }, () -> {
            TableCellFloatSlider cell = new TableCellFloatSlider("decayChaos", transformer.decayChaos, 0.0f, 1.0f);
            cell.setScale(Scales.pow(3));
            cell.addListener(val -> transformer.decayChaos = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.chaos"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.chaos.tooltip"));
        }, () -> {
            TableCellFloatSlider cell = new TableCellFloatSlider("decayValueDensity", transformer.decayValueDensity, 0.0f, 1.0f);
            cell.setScale(Scales.pow(3));
            cell.addListener(val -> transformer.decayValueDensity = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.density"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.density.tooltip"));
        }, () -> {
            TableCellEnum<EnumFacing> cell = new TableCellEnum<>("decaySide", transformer.decayDirection, TableDirections.getDirectionOptions(EnumFacing.VALUES));
            cell.addListener(val -> transformer.decayDirection = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.decay.direction"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.decay.direction.tooltip"));
        });

        addSegment(2, () -> {
            return new TitledCell(new TableCellTitle("otherTitle", IvTranslations.get("reccomplex.transformer.ruins.other.title")));
        }, () -> {
            TableCellFloatSlider cell = new TableCellFloatSlider("erosion", transformer.blockErosion, 0.0f, 1.0f);
            cell.setScale(Scales.pow(3));
            cell.addListener(val -> transformer.blockErosion = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.erosion"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.erosion.tooltip"));
        }, () -> {
            TableCellFloatSlider cell = new TableCellFloatSlider("vines", transformer.vineGrowth, 0.0f, 1.0f);
            cell.setScale(Scales.pow(3));
            cell.addListener(val -> transformer.vineGrowth = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.vines"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.vines.tooltip"));
        }, () -> {
            TableCellFloatSlider cell = new TableCellFloatSlider("cobwebs", transformer.cobwebGrowth, 0.0f, 1.0f);
            cell.setScale(Scales.pow(3));
            cell.addListener(val -> transformer.cobwebGrowth = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.cobwebs"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.cobwebs.tooltip"));
        }, () -> {
            TableCellBoolean cell = new TableCellBoolean("gravity", transformer.gravity);
            cell.addListener(val -> transformer.gravity = val);
            return new TitledCell(IvTranslations.get("reccomplex.transformer.ruins.gravity"), cell)
                    .withTitleTooltip(IvTranslations.formatLines("reccomplex.transformer.ruins.gravity.tooltip"));
        });
    }

    public TransformerRuins getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerRuins transformer)
    {
        this.transformer = transformer;
    }
}
