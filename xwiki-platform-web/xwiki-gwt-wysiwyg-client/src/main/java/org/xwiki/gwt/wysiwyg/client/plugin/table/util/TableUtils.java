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
package org.xwiki.gwt.wysiwyg.client.plugin.table.util;

import org.xwiki.gwt.dom.client.DOMUtils;
import org.xwiki.gwt.dom.client.Document;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;

/**
 * Utility class designed to ease HTML tables manipulation.
 * 
 * @version $Id$
 */
public final class TableUtils
{
    /**
     * HTML tag defining the root node of the area being edited.
     */
    public static final String BODY_NODENAME = "BODY";

    /**
     * HTML tag defining a table.
     */
    public static final String TABLE_NODENAME = "TABLE";

    /**
     * HTML tag defining a table body.
     */
    public static final String TBODY_NODENAME = "TBODY";

    /**
     * HTML tag defining a table row.
     */
    public static final String ROW_NODENAME = "TR";

    /**
     * HTML tag defining a table cell.
     */
    public static final String COL_NODENAME = "TD";

    /**
     * HTML tag defining a table heading cell.
     */
    public static final String COL_HNODENAME = "TH";

    /**
     * HTML to be inserted in newly created table cells.
     */
    public static final String CELL_DEFAULTHTML = "&nbsp;";

    // FIXME : find a better solution and delete this
    // Already tried newCell.appendChild(doc.createTextNode("")); not a good solution
    // since FF3 (and may be others) renders the caret a bit too high in that case.

    /**
     * The instance in use.
     */
    private static TableUtils instance;

    /**
     * Get the instance in use.
     * 
     * @return the instance in use.
     */
    public static synchronized TableUtils getInstance()
    {
        if (instance == null) {
            instance = new TableUtils();
        }
        return instance;
    }

    /**
     * Inspect a Document to get the currently selected Range.
     * 
     * @param doc Document to inspect.
     * @return currently selected range.
     */
    public Range getRange(Document doc)
    {
        // TODO : refine this method to take multiple ranges into account.
        return doc.getSelection().getRangeAt(0);
    }

    /**
     * Get the node in which the caret is currently positioned.
     * 
     * @param doc Document to get the caret from.
     * @return node holding the caret.
     */
    public Node getCaretNode(Document doc)
    {
        return getRange(doc).getCommonAncestorContainer();
    }

    /**
     * Put caret in the given HTML node.
     * 
     * @param rta wysiwyg RichTextArea
     * @param node the node to put the caret in.
     */
    public void putCaretInNode(RichTextArea rta, Node node)
    {
        Selection selection = rta.getDocument().getSelection();
        Range range = rta.getDocument().createRange();

        range.selectNodeContents(node);
        range.collapse(false);
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Browse node ancestors and return the first table element.
     * 
     * @param node the Node to inspect.
     * @return the matching TableElement if any, null otherwise.
     */
    public TableElement getTable(Node node)
    {
        return (TableElement) DOMUtils.getInstance().getFirstAncestor(node, TABLE_NODENAME);
    }

    /**
     * Browse node ancestors and return the first table row element.
     * 
     * @param node the Node to inspect.
     * @return the matching TableRowElement if any, null otherwise.
     */
    public TableRowElement getRow(Node node)
    {
        return (TableRowElement) DOMUtils.getInstance().getFirstAncestor(node, ROW_NODENAME);
    }

    /**
     * Determine if the row is a header row. A header row contains TH nodes describing their respective columns.
     * 
     * @param row the row to inspect.
     * @return true if the row is a heading row.
     */
    public boolean isHeaderRow(TableRowElement row)
    {
        if (row.getCells().getLength() > 0 && COL_HNODENAME.equalsIgnoreCase(row.getCells().getItem(0).getNodeName())) {
            return true;
        }
        return false;
    }

    /**
     * Browse node ancestors and return the first table cell element (TD or TH).
     * 
     * @param node the node to inspect.
     * @return the matching TableCellElement if any, null otherwise.
     */
    public TableCellElement getCell(Node node)
    {
        TableCellElement cell;
        cell = (TableCellElement) DOMUtils.getInstance().getFirstAncestor(node, COL_NODENAME);
        if (cell == null) {
            cell = (TableCellElement) DOMUtils.getInstance().getFirstAncestor(node, COL_HNODENAME);
        }
        return cell;
    }

    /**
     * Get next cell in cell's row.
     * 
     * @param cell currently edited cell.
     * @return the next TableCellElement if any, null otherwise.
     */
    public TableCellElement getNextCellInRow(TableCellElement cell)
    {
        TableRowElement row = getRow(cell);
        NodeList<TableCellElement> cells = row.getCells();
        if (cells.getLength() > cell.getCellIndex() + 1) {
            return cells.getItem(cell.getCellIndex() + 1);
        } else {
            return null;
        }
    }

    /**
     * Get previous cell in cell's row.
     * 
     * @param cell currently edited cell.
     * @return the previous TableCellElement if any, null otherwise.
     */
    public TableCellElement getPreviousCellInRow(TableCellElement cell)
    {
        TableRowElement row = getRow(cell);
        NodeList<TableCellElement> cells = row.getCells();
        if (cell.getCellIndex() > 0) {
            return cells.getItem(cell.getCellIndex() - 1);
        } else {
            return null;
        }
    }

    /**
     * Get next cell in cell's column.
     * 
     * @param cell currently edited cell.
     * @return the next TableCellElement if any, null otherwise.
     */
    public TableCellElement getNextCellInColumn(TableCellElement cell)
    {
        TableRowElement row = getRow(cell);
        TableElement table = getTable(row);
        NodeList<TableRowElement> rows = table.getRows();
        if (rows.getLength() > row.getRowIndex() + 1) {
            return rows.getItem(row.getRowIndex() + 1).getCells().getItem(cell.getCellIndex());
        } else {
            return null;
        }
    }

    /**
     * Get previous cell in cell's column.
     * 
     * @param cell currently edited cell.
     * @return the previous TableCellElement if any, null otherwise.
     */
    public TableCellElement getPreviousCellInColumn(TableCellElement cell)
    {
        TableRowElement row = getRow(cell);
        TableElement table = getTable(row);
        NodeList<TableRowElement> rows = table.getRows();
        if (row.getRowIndex() > 0) {
            return rows.getItem(row.getRowIndex() - 1).getCells().getItem(cell.getCellIndex());
        } else {
            return null;
        }
    }

    /**
     * Insert a row in the currently edited table.
     * 
     * @param doc the Document to get the selection from.
     * @param insertBefore indicates the creation position relatively to the currently edited row.
     * @return the newly created TableRowElement.
     */
    public TableRowElement insertRow(Document doc, boolean insertBefore)
    {
        TableRowElement row = getRow(getCaretNode(doc));
        TableElement table = getTable(row);
        int index = row.getRowIndex();

        if (!insertBefore) {
            index++;
        }

        TableRowElement newRow = table.insertRow(index);
        // Populate the row
        int cellCount = row.getCells().getLength();
        for (int i = 0; i < cellCount; i++) {
            TableCellElement cell = newRow.insertCell(i);
            cell.setInnerHTML(CELL_DEFAULTHTML);
        }

        return newRow;
    }

    /**
     * Insert a column in the currently edited table.
     * 
     * @param doc the Document to get the selection from.
     * @param insertBefore indicates the creation position relatively to the currently edited column.
     */
    public void insertCol(Document doc, boolean insertBefore)
    {
        TableCellElement cell = getCell(getCaretNode(doc));
        TableElement table = getTable(cell);
        NodeList<TableRowElement> rows = table.getRows();
        int index = cell.getCellIndex();

        // Loop over table rows to create a new cell in each of them
        for (int i = 0; i < rows.getLength(); i++) {
            TableRowElement currentRow = rows.getItem(i);
            TableCellElement newCell;

            if (isHeaderRow(currentRow)) {
                newCell = doc.createTHElement();
                if (insertBefore) {
                    currentRow.insertBefore(newCell, currentRow.getCells().getItem(index));
                } else {
                    DOMUtils.getInstance().insertAfter(newCell, currentRow.getCells().getItem(index));
                }
            } else {
                newCell = currentRow.insertCell(insertBefore ? index : index + 1);
            }
            newCell.setInnerHTML(CELL_DEFAULTHTML);
        }
    }
}
