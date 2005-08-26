package com.xpn.xwiki.plugin.charts.source;

import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;

public interface DataSource {
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
