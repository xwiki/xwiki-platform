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
package org.xwiki.gwt.dom.client;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.TableElement;

/**
 * Extends the implementation provided by GWT to add useful methods. All of them should be removed as soon as they make
 * their way into GWT's API.
 * 
 * @version $Id$
 */
public final class TableRowElement extends com.google.gwt.dom.client.TableRowElement
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected TableRowElement()
    {
        super();
    }

    /**
     * @return The table containing this cell.
     */
    public TableElement getOwnerTable()
    {
        Node ancestor = this.getParentNode();
        while (ancestor != null && !"table".equalsIgnoreCase(ancestor.getNodeName())) {
            ancestor = ancestor.getParentNode();
        }
        return (TableElement) ancestor;
    }

    /**
     * @return The next row inside the same table. If this is the last row then it returns null.
     */
    public TableRowElement getNextRow()
    {
        TableElement table = getOwnerTable();
        if (getRowIndex() < table.getRows().getLength() - 1) {
            return (TableRowElement) table.getRows().getItem(getRowIndex() + 1);
        } else {
            return null;
        }
    }

    /**
     * @return The previous row inside the same table. If this is the first row then it returns null.
     */
    public TableRowElement getPreviousRow()
    {
        TableElement table = getOwnerTable();
        if (getRowIndex() > 0) {
            return (TableRowElement) table.getRows().getItem(getRowIndex() - 1);
        } else {
            return null;
        }
    }

    /**
     * @return The first cell of this row.
     */
    public TableCellElement getFirstCell()
    {
        return (TableCellElement) getCells().getItem(0);
    }

    /**
     * @return The last cell of this row.
     */
    public TableCellElement getLastCell()
    {
        return (TableCellElement) getCells().getItem(getCells().getLength() - 1);
    }
}
