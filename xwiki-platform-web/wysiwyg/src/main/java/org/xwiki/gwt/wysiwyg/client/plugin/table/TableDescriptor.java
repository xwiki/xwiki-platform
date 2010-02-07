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
package org.xwiki.gwt.wysiwyg.client.plugin.table;

/**
 * Describes a table.
 * 
 * @version $Id$
 */
public class TableDescriptor
{
    /**
     * The number of rows.
     */
    private int rowCount;

    /**
     * The number of columns.
     */
    private int columnCount;

    /**
     * Specifies if the table has a header.
     */
    private boolean withHeader;

    /**
     * @return the number of rows
     */
    public int getRowCount()
    {
        return rowCount;
    }

    /**
     * Sets the number of rows.
     * 
     * @param rowCount the number of rows
     */
    public void setRowCount(int rowCount)
    {
        this.rowCount = rowCount;
    }

    /**
     * @return the number of columns
     */
    public int getColumnCount()
    {
        return columnCount;
    }

    /**
     * Sets the number of columns.
     * 
     * @param columnCount the number of columns
     */
    public void setColumnCount(int columnCount)
    {
        this.columnCount = columnCount;
    }

    /**
     * @return {@code true} if the table has a header, {@code false} otherwise
     */
    public boolean isWithHeader()
    {
        return withHeader;
    }

    /**
     * Specifies if the table has a header or not.
     * 
     * @param withHeader {@code true} to have a header, {@code false} otherwise
     */
    public void setWithHeader(boolean withHeader)
    {
        this.withHeader = withHeader;
    }
}
