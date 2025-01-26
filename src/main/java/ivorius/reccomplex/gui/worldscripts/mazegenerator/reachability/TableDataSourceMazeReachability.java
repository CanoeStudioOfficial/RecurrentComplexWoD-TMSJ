/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.worldscripts.mazegenerator.reachability;

import ivorius.ivtoolkit.tools.IvTranslations;
import ivorius.reccomplex.client.rendering.MazeVisualizationContext;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.TableCellTitle;
import ivorius.reccomplex.gui.table.cell.TitledCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourcePreloaded;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.world.gen.feature.structure.generic.Selection;
import ivorius.reccomplex.world.gen.feature.structure.generic.maze.SavedMazePath;
import ivorius.reccomplex.world.gen.feature.structure.generic.maze.SavedMazeReachability;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.Set;

/**
 * Created by lukas on 16.03.16.
 */

@SideOnly(Side.CLIENT)
public class TableDataSourceMazeReachability extends TableDataSourceSegmented
{
    protected SavedMazeReachability reachability;

    private TableDelegate tableDelegate;
    private TableNavigator tableNavigator;

    public TableDataSourceMazeReachability(SavedMazeReachability reachability, MazeVisualizationContext visualizationContext, TableDelegate tableDelegate, TableNavigator tableNavigator, Set<SavedMazePath> expected, Selection bounds)
    {
        this.reachability = reachability;
        this.tableDelegate = tableDelegate;
        this.tableNavigator = tableNavigator;

        addSegment(0, new TableDataSourcePreloaded(new TitledCell(
                new TableCellTitle("", IvTranslations.get("reccomplex.reachability.groups")))
                .withTitleTooltip(IvTranslations.formatLines("reccomplex.reachability.groups.tooltip"))));
        addSegment(1, new TableDataSourceMazeReachabilityGrouping(reachability, expected, tableDelegate, tableNavigator));

        addSegment(2, new TableDataSourcePreloaded(new TitledCell(
                new TableCellTitle("", IvTranslations.get("reccomplex.reachability.crossconnections")))
                .withTitleTooltip(IvTranslations.formatLines("reccomplex.reachability.crossconnections.tooltip"))));
        addSegment(3, new TableDataSourceMazePathPairList(reachability.crossConnections, tableDelegate, tableNavigator, bounds)
                .visualizing(visualizationContext));
    }

    public SavedMazeReachability getReachability()
    {
        return reachability;
    }

    public void setReachability(SavedMazeReachability reachability)
    {
        this.reachability = reachability;
    }

    public TableDelegate getTableDelegate()
    {
        return tableDelegate;
    }

    public void setTableDelegate(TableDelegate tableDelegate)
    {
        this.tableDelegate = tableDelegate;
    }

    public TableNavigator getTableNavigator()
    {
        return tableNavigator;
    }

    public void setTableNavigator(TableNavigator tableNavigator)
    {
        this.tableNavigator = tableNavigator;
    }

    @Nonnull
    @Override
    public String title()
    {
        return "Reachability";
    }
}
