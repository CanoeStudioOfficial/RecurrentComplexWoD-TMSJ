/*
 *  Copyright (c) 2014, Lukas Tenbrink.
 *  * http://lukas.axxim.net
 */

package ivorius.reccomplex.gui.worldscripts.command;

import ivorius.reccomplex.gui.table.TableCells;
import ivorius.reccomplex.gui.table.TableDelegate;
import ivorius.reccomplex.gui.table.TableNavigator;
import ivorius.reccomplex.gui.table.cell.TableCell;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceList;
import ivorius.reccomplex.gui.table.datasource.TableDataSourceSegmented;
import ivorius.reccomplex.gui.worldscripts.TableDataSourceWorldScript;
import ivorius.reccomplex.world.gen.script.WorldScriptCommand;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Created by lukas on 05.06.14.
 */

@SideOnly(Side.CLIENT)
public class TableDataSourceWorldScriptCommand extends TableDataSourceSegmented
{
    public TableDataSourceWorldScriptCommand(WorldScriptCommand script, TableDelegate tableDelegate, TableNavigator navigator)
    {
        addSegment(0, new TableDataSourceWorldScript(script));

        addSegment(1, new TableDataSourceList<WorldScriptCommand.Entry, List<WorldScriptCommand.Entry>>(script.entries, tableDelegate, navigator)
        {
            @Override
            public String getDisplayString(WorldScriptCommand.Entry entry)
            {
                return entry.command;
            }

            @Override
            public WorldScriptCommand.Entry newEntry(String actionID)
            {
                return new WorldScriptCommand.Entry(1.0, "");
            }

            @Nonnull
            @Override
            public TableCell entryCell(boolean enabled, WorldScriptCommand.Entry entry)
            {
                return TableCells.edit(enabled, navigator, tableDelegate, () -> new TableDataSourceSpawnCommandEntry(entry, tableDelegate));
            }
        });
    }
}
