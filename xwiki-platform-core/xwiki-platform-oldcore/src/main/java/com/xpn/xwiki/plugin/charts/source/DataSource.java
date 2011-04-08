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
 *
 */
package com.xpn.xwiki.plugin.charts.source;

import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public interface DataSource
{
    int getRowCount() throws DataSourceException;

    int getColumnCount() throws DataSourceException;

    Number getCell(int rowIndex, int colIndex) throws DataSourceException;

    Number[] getRow(int rowIndex) throws DataSourceException;

    Number[] getColumn(int colIndex) throws DataSourceException;

    Number[][] getAllCells() throws DataSourceException;

    boolean hasHeaderRow() throws DataSourceException;

    boolean hasHeaderColumn() throws DataSourceException;

    String getHeaderRowValue(int columnIndex) throws DataSourceException;

    String[] getHeaderRow() throws DataSourceException;

    String getHeaderColumnValue(int rowIndex) throws DataSourceException;

    String[] getHeaderColumn() throws DataSourceException;
}
