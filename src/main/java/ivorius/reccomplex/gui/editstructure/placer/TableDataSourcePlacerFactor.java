/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.editstructure.placer;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.TableCellFloatSlider;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSupplied;
import ivorius.reccomplex.world.gen.feature.structure.generic.placement.GenericPlacer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

/**
 * Created by lukas on 04.04.15.
 */

@SideOnly(Side.CLIENT)
public class TableDataSourcePlacerFactor extends TableDataSourceSegmented
{
    public GenericPlacer.Factor placer;

    public TableDataSourcePlacerFactor(GenericPlacer.Factor factor, TableDelegate delegate, TableNavigator navigator)
    {
        this.placer = factor;

        addSegment(0, new TableDataSourceSupplied(() -> {
            TableCellFloatSlider priority = new TableCellFloatSlider(null, factor.priority, 0, 10);
            priority.addListener(v -> factor.priority = v);
            return new TitledCell(IvTranslations.get("reccomplex.placer.factor.priority"), priority)
                    .withTitleTooltip(IvTranslations.getLines("reccomplex.placer.factor.priority.tooltip"));
        }));
    }

    @Nonnull
    @Override
    public String title()
    {
        return placer.displayString();
    }
}
