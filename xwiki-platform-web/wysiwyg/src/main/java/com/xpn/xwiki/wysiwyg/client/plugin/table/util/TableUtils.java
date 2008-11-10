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
package com.xpn.xwiki.wysiwyg.client.plugin.table.util;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.TableCellElement;
import com.google.gwt.dom.client.TableElement;
import com.google.gwt.dom.client.TableRowElement;
import com.xpn.xwiki.wysiwyg.client.dom.DOMUtils;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Document;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;
import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;

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
     * HTML tag defining a table row.
     */
    public static final String ROW_NODENAME = "TR";

    /**
     * HTML tag defining a table cell.
     */
    public static final String COL_NODENAME = "TD";

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
     * Inspect a node to determine if it has a valid parent. A valid parent must be different from null and from the
     * editable area root node.
     * 
     * @param node Node to inspect.
     * @return true if the node has a valid parent, false otherwise.
     */
    private boolean hasParent(Node node)
    {
        return node.getParentNode() != null && !BODY_NODENAME.equals(node.getParentNode().getNodeName());
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
     * Browse node ancestors and return the first table cell element.
     * 
     * @param node the node to inspect.
     * @return the matching TableCellElement if any, null otherwise.
     */
    public TableCellElement getCell(Node node)
    {        
        return (TableCellElement) DOMUtils.getInstance().getFirstAncestor(node, COL_NODENAME);
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

        if (!insertBefore) {
            index++;
        }

        // Loop over table rows to create a new cell in each of them
        for (int i = 0; i < rows.getLength(); i++) {
            TableRowElement currentRow = rows.getItem(i);
            TableCellElement newCell = currentRow.insertCell(index);
            newCell.setInnerHTML(CELL_DEFAULTHTML);
        }
    }
}
