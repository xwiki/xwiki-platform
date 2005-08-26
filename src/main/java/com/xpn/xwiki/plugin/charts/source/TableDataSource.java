package com.xpn.xwiki.plugin.charts.source;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.radeox.macro.table.Table;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.charts.RadeoxHelper;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.EmptyDataSourceException;

public class TableDataSource extends DefaultDataSource implements DataSource {
    public static final String TABLE_INDEX = "tableIndex";
    public static final String RANGE= "range";
    public static final String HAS_HEADER_ROW = "hasHeaderRow";
    public static final String HAS_HEADER_COLUMN = "hasHeaderColumn";
    
    private int startColumn = -1;
    private int endColumn = -1;
    private int startRow = -1;
    private int endRow = -1;
    private int headerColumnIndex = -1;
    private int headerRowIndex = -1;

    public TableDataSource(BaseObject defObject, XWikiContext context)
    		throws DataSourceException {
        try {
             XWikiDocument ownerDocument = context.getWiki()
            		.getDocument(defObject.getName(), context);
            RadeoxHelper rHelper = new RadeoxHelper(ownerDocument, context);
            int index = defObject.getIntValue(TABLE_INDEX);
            Table table = rHelper.getTable(index);
            parseRange(defObject.getStringValue(RANGE),
            		defObject.getIntValue(HAS_HEADER_ROW) == 1,
            		defObject.getIntValue(HAS_HEADER_COLUMN) == 1, table);
            makeDataMatrix(table);
            makeHeaders(table);
        } catch (XWikiException e) {
        	throw new DataSourceException(e);
        } catch (NumberFormatException e) {
        	throw new DataSourceException(e);
        }
    }

    private void makeDataMatrix(Table t) {
        data = new Number[endRow - startRow + 1][endColumn - startColumn + 1];
        for(int y = startRow; y <= endRow; y++){
            for(int x = startColumn; x <= endColumn; x++){
                try{
                    data[y - startRow][x - startColumn] = toNumber(t.getXY(x, y).toString());
                } catch (Exception e) {
                    data[y - startRow][x - startColumn] = null;
                }
            }
        }
    }
    
    private void makeHeaders(Table t) {
        if (headerColumnIndex >= 0){
            headerColumn = new String[endRow - startRow + 1];
            for(int y = startRow; y <= endRow; y++){
                try{
                    headerColumn[y - startRow] = t.getXY(headerColumnIndex, y).toString();
                } catch (Exception e) {
                    headerColumn[y - startRow] = "";
                }
            }
        }
        if(headerRowIndex >= 0){
            headerRow = new String[endColumn - startColumn + 1];
            for(int x = startColumn; x <= endColumn; x++){
                try{
                    headerRow[x - startColumn] = t.getXY(x, headerRowIndex).toString();
                } catch(Exception ex){
                    headerRow[x - startColumn] = "";
                }
            }
        }
    }

    private static Number toNumber(String str) throws NumberFormatException {
        try {
            return new Long(str);
        }
        catch (NumberFormatException e1) {
            try {
                return new Double(str);
            }
            catch (NumberFormatException e2) {
                try {
                    new BigInteger(str);
                }
                catch (NumberFormatException e3) {
                    try{
                        return new BigDecimal(str);
                    }
                    catch (NumberFormatException e4) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Valid string formats:
     * Xm:Yn  => rectangular range
     * X:Y    => whole columns range
     * m:n    => whole rows range
     * *      => enire table
     * Where X, Y are Uppercase letters, m, n are numbers
     * First column is A, first row is 1
     */
    private void parseRange(String range, boolean hasHeaderRow,
    		boolean hasHeaderColumn, Table t) throws DataSourceException{
        if(range.matches("[A-Z][0-9]+:[A-Z][0-9]+")){
            startColumn = (int)range.charAt(0) - (int)'A';
            startRow = Integer.parseInt(range.substring(1, range.indexOf(':'))) - 1;
            endColumn = (int)range.charAt(range.indexOf(':') + 1) - (int)'A';
            endRow = Integer.parseInt(range.substring(range.indexOf(':') + 2)) - 1;
        }
        else if(range.matches("[A-Z]:[A-Z]")){
            startColumn = (int)range.charAt(0) - (int)'A';
            startRow = 0;
            endColumn = (int)range.charAt(range.indexOf(':') + 1) - (int)'A';
            endRow = getTableRowCount(t);
        }
        else if(range.matches("[0-9]+:[0-9]+")){
            startColumn = 0;
            startRow = Integer.parseInt(range.substring(0, range.indexOf(':'))) - 1;
            endColumn = getTableColumnCount(t);
            endRow = Integer.parseInt(range.substring(range.indexOf(':') + 1)) - 1;
        }
        else if(range.equals("*")){
            startColumn = 0;
            startRow = 0;
            endColumn = getTableColumnCount(t);
            endRow = getTableRowCount(t);
        }
        if(hasHeaderColumn){
            this.headerColumnIndex = startColumn++;
            if(startColumn > endColumn){
                throw new EmptyDataSourceException("Data source cannot contain only a header");
            }
        }
        if(hasHeaderRow){
            this.headerRowIndex = startRow++;
            if(startRow > endRow){
                throw new EmptyDataSourceException("Data source cannot contain only a header");
            }
        }
    }

    private int getTableColumnCount(Table t) throws DataSourceException{
        int i = 0;
        try{
            while(true){
                t.getXY(i, 0);
                i++;
            }
        }
        catch(Exception ex){
            // Reached the table limit
            i--;
        }
        if(i < 0){
            throw new EmptyDataSourceException();
        }
        return i;
    }

    private int getTableRowCount(Table t) throws DataSourceException{
        int i = 0;
        try{
            while(true){
                t.getXY(0, i);
                i++;
            }
        }
        catch(Exception ex){
            // Reached the table limit
            i--;
        }
        if(i < 0){
            throw new EmptyDataSourceException();
        }
        return i;
    }
}
