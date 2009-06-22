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
package org.xwiki.chart.model;

/**
 * Interface defining the input for the chart generator.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public interface ChartModel
{
    /**
     * Number of data rows.
     * 
     * @return number of data rows present in this model.
     */
    int getRowCount();

    /**
     * Number of data columns.
     * 
     * @return number of data columns present in this model.
     */
    int getColumnCount();

    /**
     * Returns the specified cell value.
     * 
     * @param rowIndex row index.
     * @param columnIndex column index.
     * @return cell value.
     */
    Number getCellValue(int rowIndex, int columnIndex);

    /**
     * Returns the label for the specified data row.
     * 
     * @param rowIndex row index.
     * @return the label for the specified data row.
     */
    String getRowHeader(int rowIndex);

    /**
     * Returns the label for the specified data column.
     * 
     * @param columnIndex column index.
     * @return the label for the specified data column.
     */
    String getColumnHeader(int columnIndex);
}
