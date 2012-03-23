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
 * Default implementation of {@link ChartModel}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultChartModel implements ChartModel
{
    /**
     * @see ChartModel#getRowCount()
     */
    private int rowCount;

    /**
     * @see ChartModel#getColumnCount()
     */
    private int columnCount;

    /**
     * Two dimensional array holding the chart data.
     */
    private Number[][] data;

    /**
     * @see ChartModel#getRowHeader(int)
     */
    private String[] rowHeaders;

    /**
     * @see ChartModel#getColumnHeader(int)
     */
    private String[] columnHeaders;

    /**
     * Constructs a new {@link DefaultChartModel} with the provided data.
     * 
     * @param data chart data.
     */
    public DefaultChartModel(Number[][] data)
    {
        rowCount = data.length;
        columnCount = (rowCount > 0) ? data[0].length : 0;
        if (rowCount == 0 || columnCount == 0) {
            throw new IllegalArgumentException("Invalid row count / column count.");
        }
        for (Number[] row : data) {
            if (row.length != columnCount) {
                throw new IllegalArgumentException("Incomplete data.");
            }
        }
        this.data = data;
    }

    /**
     * Constructs a new {@link DefaultChartModel} with the provided data and row / column headers.
     * 
     * @param data chart data.
     * @param rowHeaders row headers.
     * @param columnHeaders column headers.
     */
    public DefaultChartModel(Number[][] data, String[] rowHeaders, String[] columnHeaders)
    {
        this(data);
        if (rowHeaders != null && rowHeaders.length != rowCount) {
            throw new IllegalArgumentException("Incomplete row headers.");
        }
        if (columnHeaders != null && columnHeaders.length != columnCount) {
            throw new IllegalArgumentException("Incomplete column headers.");
        }
        this.rowHeaders = rowHeaders;
        this.columnHeaders = columnHeaders;
    }

    @Override
    public int getRowCount()
    {
        return rowCount;
    }

    @Override
    public int getColumnCount()
    {
        return columnCount;
    }

    @Override
    public Number getCellValue(int rowIndex, int columnIndex)
    {
        if (rowIndex >= 0 && rowIndex < rowCount && columnIndex >= 0 && columnIndex < columnCount) {
            return data[rowIndex][columnIndex];
        }
        throw new IllegalArgumentException(String.format("Invalid cell specified : (%d, %d).", rowIndex, columnIndex));
    }

    @Override
    public String getRowHeader(int rowIndex)
    {
        if (rowIndex >= 0 && rowIndex < rowCount) {
            return (rowHeaders != null) ? rowHeaders[rowIndex] : ("R" + rowIndex);
        }
        throw new IllegalArgumentException(String.format("Invalid row index : [%s].", rowIndex));
    }

    @Override
    public String getColumnHeader(int columnIndex)
    {
        if (columnIndex >= 0 && columnIndex < columnCount) {
            return (columnHeaders != null) ? columnHeaders[columnIndex] : ("C" + columnIndex);
        }
        throw new IllegalArgumentException(String.format("Invalid column index : [%s].", columnIndex));
    }
}
