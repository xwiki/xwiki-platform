/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.wysiwyg.client.plugin.table.feature;

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.plugin.table.TablePlugin;
import org.xwiki.gwt.wysiwyg.client.plugin.table.util.TableUtils;

import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;

/**
 * Feature allowing to remove a column from a table. Before removal the caret is moved to the previous cell in the row.
 * It is disabled when the caret is positioned outside of a column (cell element).
 * 
 * @version $Id$
 */
public class DeleteCol extends AbstractTableFeature
{
    /**
     * Feature name.
     */
    public static final String NAME = "deletecol";

    /**
     * Initialize the feature. Table features needs to be aware of the plug-in (here the ClickListener) since they hold
     * their own PushButton.
     * 
     * @param plugin table plug-in.
     */
    public DeleteCol(TablePlugin plugin)
    {
        super(NAME, new Command(NAME), Strings.INSTANCE.deleteCol(), plugin);
    }

    @Override
    public boolean execute(String parameter)
    {
        TableCellElement caretCell =
            TableUtils.getInstance().getCell(TableUtils.getInstance().getCaretNode(rta.getDocument()));
        int cellIndex = caretCell.getCellIndex();
        TableElement table = TableUtils.getInstance().getTable(caretCell);
        NodeList<TableRowElement> rows = table.getRows();

        // Move caret
        TableCellElement newCaretCell = TableUtils.getInstance().getPreviousCellInRow(caretCell);
        if (newCaretCell == null) {
            newCaretCell = TableUtils.getInstance().getNextCellInRow(caretCell);
        }
        if (newCaretCell != null) {
            TableUtils.getInstance().putCaretInNode(rta, newCaretCell);
        }

        // Loop over table rows to delete a cell in each of them
        for (int i = 0; i < rows.getLength(); i++) {
            TableRowElement currentRow = rows.getItem(i);
            currentRow.deleteCell(cellIndex);
        }

        return true;
    }

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled()
            && TableUtils.getInstance().getCell(TableUtils.getInstance().getCaretNode(rta.getDocument())) != null;
    }
}
