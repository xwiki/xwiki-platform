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

/**
 * Extends the implementation provided by GWT to add useful methods. All of them should be removed as soon as they make
 * their way into GWT's API.
 * 
 * @version $Id$
 */
public final class TableCellElement extends com.google.gwt.dom.client.TableCellElement
{
    /**
     * Default constructor. Needs to be protected because all instances are created from JavaScript.
     */
    protected TableCellElement()
    {
        super();
    }

    /**
     * @return The row containing this cell.
     */
    public TableRowElement getOwnerRow()
    {
        return (TableRowElement) getParentElement();
    }

    /**
     * @return The next cell inside the same table. If this is the last cell then it returns null.
     */
    public TableCellElement getNextCell()
    {
        Node nextCell = getNextSiblingElement();
        if (nextCell == null) {
            TableRowElement nextRow = getOwnerRow().getNextRow();
            if (nextRow != null) {
                nextCell = nextRow.getFirstCell();
            }
        }
        return (TableCellElement) nextCell;
    }

    /**
     * @return The previous cell inside the same table. If this is the first cell then it returns null.
     */
    public TableCellElement getPreviousCell()
    {
        Node prevCell = getPreviousSibling();
        if (prevCell == null) {
            TableRowElement prevRow = getOwnerRow().getPreviousRow();
            if (prevRow != null) {
                prevCell = prevRow.getLastCell();
            }
        }
        return (TableCellElement) prevCell;
    }
}
