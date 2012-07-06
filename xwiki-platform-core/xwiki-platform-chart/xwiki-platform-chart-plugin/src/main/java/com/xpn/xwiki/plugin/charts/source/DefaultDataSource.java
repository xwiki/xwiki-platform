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
package com.xpn.xwiki.plugin.charts.source;

import com.xpn.xwiki.plugin.charts.exceptions.ColumnIndexOutOfBoundsException;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.NoHeaderColumnException;
import com.xpn.xwiki.plugin.charts.exceptions.NoHeaderRowException;
import com.xpn.xwiki.plugin.charts.exceptions.RowIndexOutOfBoundsException;

public class DefaultDataSource implements DataSource
{
    protected Number[][] data;

    protected String[] headerRow;

    protected String[] headerColumn;

    /**
     * This no-arg constructor creates an empty data source with no headers.
     */
    public DefaultDataSource()
    {
        data = new Number[0][0];
    }

    /**
     * This constructor creates a data source with no headers
     * 
     * @param data A matrix containing the values of the data source
     */
    public DefaultDataSource(Number[][] data)
    {
        this.data = data;
    }

    /**
     * This constructor creates a data source with the given data and headers
     * 
     * @param data A matrix containing the values of the data source
     * @param headerRow The header row with headerRow.length == data[x].length, for x=0,data.length-1
     * @param headerColumn The header column with headerColumn.length == data.length
     * @throws IllegalArgumentException Thrown when the conditions above are not satisfied
     */
    public DefaultDataSource(Number[][] data, String[] headerRow, String[] headerColumn)
    {
        if (headerColumn != null && headerColumn.length != data.length) {
            throw new IllegalArgumentException("headerColumn.length != data.length");
        }
        for (int i = 0; i < data.length; i++) {
            if (headerRow != null && headerRow.length != data[i].length) {
                throw new IllegalArgumentException("headerRow.length != data[" + i + "].length");
            } else if (headerRow == null && i > 0 && data[i].length != data[i - 1].length) {
                throw new IllegalArgumentException("data[" + i + "].length != data[" + (i - 1) + "].length");
            }
        }
        this.data = data;
        this.headerColumn = headerColumn;
        this.headerRow = headerRow;
    }

    /**
     * The number of rows of this data source
     */
    @Override
    public int getRowCount()
    {
        return data.length;
    }

    /**
     * The number of columns of this data source
     */
    @Override
    public int getColumnCount()
    {
        if (data.length > 0) {
            return data[0].length;
        } else {
            return 0;
        }
    }

    /**
     * @return The value of a single cell
     * @throws RowIndexOutOfBoundsException
     * @throws ColumnIndexOutOfBoundsException
     */
    @Override
    public Number getCell(int rowIndex, int colIndex) throws DataSourceException
    {
        checkRowIndex(rowIndex);
        checkColumnIndex(colIndex);
        return data[rowIndex][colIndex];
    }

    /**
     * Sets the value of a single cell
     * 
     * @throws RowIndexOutOfBoundsException
     * @throws ColumnIndexOutOfBoundsException
     */
    public void setCell(int rowIndex, int colIndex, Number content) throws DataSourceException
    {
        checkRowIndex(rowIndex);
        checkColumnIndex(colIndex);
        data[rowIndex][colIndex] = content;
    }

    /**
     * @return A whole row
     * @throws RowIndexOutOfBoundsException
     */
    @Override
    public Number[] getRow(int rowIndex) throws DataSourceException
    {
        checkRowIndex(rowIndex);
        return data[rowIndex];
    }

    /**
     * @return A whole column
     * @throws ColumnIndexOutOfBoundsException
     */
    @Override
    public Number[] getColumn(int colIndex) throws DataSourceException
    {
        checkColumnIndex(colIndex);
        Number[] column = new Number[getRowCount()];
        for (int i = 0; i < getRowCount(); i++) {
            column[i] = data[i][colIndex];
        }
        return column;
    }

    /**
     * @return A matrix containing the all data source values
     */
    @Override
    public Number[][] getAllCells() throws DataSourceException
    {
        return data;
    }

    /**
     * @return true when this data source has a header row
     */
    @Override
    public boolean hasHeaderRow() throws DataSourceException
    {
        return headerRow != null;
    }

    /**
     * @return true when this data source has a header column
     */
    @Override
    public boolean hasHeaderColumn() throws DataSourceException
    {
        return headerColumn != null;
    }

    /**
     * @return the value in the header row, given by columnIndex
     * @throws NoHeaderRowException
     * @throws ColumnIndexOutOfBoundsException
     */
    @Override
    public String getHeaderRowValue(int columnIndex) throws DataSourceException
    {
        checkHeaderRow();
        checkColumnIndex(columnIndex);
        return headerRow[columnIndex];
    }

    /**
     * @return The whole header row
     * @throws NoHeaderRowException
     */
    @Override
    public String[] getHeaderRow() throws DataSourceException
    {
        checkHeaderRow();
        return headerRow;
    }

    /**
     * @return the value in the header column, given by rowIndex
     * @throws NoHeaderColumnException
     * @throws RowIndexOutOfBoundsException
     */
    @Override
    public String getHeaderColumnValue(int rowIndex) throws DataSourceException
    {
        checkHeaderColumn();
        checkRowIndex(rowIndex);
        return headerColumn[rowIndex];
    }

    /**
     * @return The whole header column
     * @throws NoHeaderColumnException
     */
    @Override
    public String[] getHeaderColumn() throws DataSourceException
    {
        checkHeaderColumn();
        return headerColumn;
    }

    private void checkRowIndex(int rowIndex) throws RowIndexOutOfBoundsException
    {
        if (rowIndex < 0 || rowIndex >= getRowCount()) {
            throw new RowIndexOutOfBoundsException("Invalid row index: " + rowIndex);
        }
    }

    private void checkColumnIndex(int columnIndex) throws ColumnIndexOutOfBoundsException
    {
        if (columnIndex < 0 || columnIndex >= getColumnCount()) {
            throw new ColumnIndexOutOfBoundsException("Invalid column index: " + columnIndex);
        }
    }

    private void checkHeaderRow() throws DataSourceException
    {
        if (!hasHeaderRow()) {
            throw new NoHeaderRowException();
        }
    }

    private void checkHeaderColumn() throws DataSourceException
    {
        if (!hasHeaderColumn()) {
            throw new NoHeaderColumnException();
        }
    }
}
