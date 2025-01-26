/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://ivorius.net
 */

package ivorius.reccomplex.gui.editstructure.transformers;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.gui.GuiValidityStateIndicator;
import ivorius.reccomplex.gui.TableDataSourceExpression;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.TableCellString;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSupplied;
import ivorius.reccomplex.world.gen.feature.structure.generic.transformers.TransformerProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

/**
 * Created by lukas on 05.06.14.
 */

@SideOnly(Side.CLIENT)
public class TableDataSourceBTProperty extends TableDataSourceSegmented
{
    private TransformerProperty transformer;

    private TableNavigator navigator;
    private TableDelegate tableDelegate;

    private TableCellString propertyIDCell;
    private TableCellString propertyValueCell;

    public TableDataSourceBTProperty(TransformerProperty transformer, TableNavigator navigator, TableDelegate delegate)
    {
        this.transformer = transformer;
        this.navigator = navigator;
        this.tableDelegate = delegate;

        addSegment(0, new TableDataSourceTransformer(transformer, delegate, navigator));
        addSegment(1, TableDataSourceExpression.constructDefault(IvTranslations.get("reccomplex.gui.sources"), IvTranslations.getLines("reccomplex.transformer.block.source.tooltip"), transformer.sourceMatcher, null));

        addSegment(2, new TableDataSourceSupplied(() -> {
            propertyIDCell = new TableCellString(null, transformer.propertyName);
            propertyIDCell.setShowsValidityState(true);
            propertyIDCell.setValidityState(currentIDState());
            propertyIDCell.addListener(v -> {
                transformer.propertyName = v;
                propertyIDCell.setValidityState(currentIDState());
                if (propertyValueCell != null)
                    propertyValueCell.setValidityState(currentValueState());
            });
            return new TitledCell(IvTranslations.get("reccomplex.transformer.propertyReplace.id"), propertyIDCell);
        }));

        addSegment(3, new TableDataSourceSupplied(() -> {
            propertyValueCell = new TableCellString(null, transformer.propertyValue);
            propertyValueCell.setShowsValidityState(true);
            propertyValueCell.setValidityState(currentValueState());
            propertyValueCell.addListener(v -> {
                transformer.propertyValue = v;
                propertyValueCell.setValidityState(currentValueState());
            });
            return new TitledCell(IvTranslations.get("reccomplex.transformer.propertyReplace.value"), propertyValueCell);
        }));
    }

    @Nonnull
    public GuiValidityStateIndicator.State currentIDState()
    {
        return TransformerProperty.propertyNameStream().anyMatch(s -> s.equals(propertyIDCell.getPropertyValue()))
                ? GuiValidityStateIndicator.State.VALID : GuiValidityStateIndicator.State.INVALID;
    }

    @Nonnull
    public GuiValidityStateIndicator.State currentValueState()
    {
        return TransformerProperty.propertyValueStream(propertyIDCell.getPropertyValue())
                .anyMatch(s -> s.equals(propertyValueCell.getPropertyValue()))
                ? GuiValidityStateIndicator.State.VALID : GuiValidityStateIndicator.State.INVALID;
    }

    public TransformerProperty getTransformer()
    {
        return transformer;
    }

    public void setTransformer(TransformerProperty transformer)
    {
        this.transformer = transformer;
    }
}
