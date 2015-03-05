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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import org.radeox.macro.table.Table;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.charts.RadeoxHelper;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.EmptyDataSourceException;

public class TableDataSource extends DefaultDataSource implements DataSource
{
    public static final String DOC = "doc";

    public static final String TABLE_NUMBER = "table_number";

    public static final String RANGE = "range";

    public static final String HAS_HEADER_ROW = "has_header_row";

    public static final String HAS_HEADER_COLUMN = "has_header_column";

    public static final String DECIMAL_SYMBOL = "decimal_symbol";

    public static final String IGNORE_ALPHA = "ignore_alpha";

    public static final String COMMA_SELECTOR = "comma";

    public static final String PERIOD_SELECTOR = "period";

    public static final int DEFAULT_TABLE_NUMBER = 0;

    public static final String DEFAULT_RANGE = "*";

    public static final boolean DEFAULT_HAS_HEADER_ROW = true;

    public static final boolean DEFAULT_HAS_HEADER_COLUMN = true;

    public static final String DEFAULT_DECIMAL_SYMBOL = PERIOD_SELECTOR;

    public static final boolean DEFAULT_IGNORE_ALPHA = false;

    public static final char COMMA = ',';

    public static final char PERIOD = '.';

    public static final char RANGE_SEP = '-';

    private int startColumn = -1;

    private int endColumn = -1;

    private int startRow = -1;

    private int endRow = -1;

    private int headerColumnIndex = -1;

    private int headerRowIndex = -1;

    private char decimalSymbol = PERIOD;

    private char digitGroupingSymbol = COMMA;

    private boolean ignoreAlpha = false;

    public TableDataSource(BaseObject defObject, XWikiContext context) throws DataSourceException
    {
        init(defObject.getName(), defObject.getIntValue(TABLE_NUMBER), defObject.getStringValue(RANGE) == ""
            ? DEFAULT_RANGE : defObject.getStringValue(RANGE), defObject.getIntValue(HAS_HEADER_ROW) == 1, defObject
            .getIntValue(HAS_HEADER_COLUMN) == 1, defObject.getStringValue(DECIMAL_SYMBOL) == ""
            ? DEFAULT_DECIMAL_SYMBOL : defObject.getStringValue(DECIMAL_SYMBOL),
            defObject.getIntValue(IGNORE_ALPHA) == 1, context);
    }

    public TableDataSource(Map params, XWikiContext context) throws DataSourceException
    {
        String doc = (String) params.get(DOC);
        if (doc == null) {
            doc = context.getDoc().getFullName();
        }

        String n = (String) params.get(TABLE_NUMBER);
        int number;
        if (n != null) {
            try {
                number = Integer.parseInt(n);
            } catch (NumberFormatException e) {
                throw new DataSourceException("Invalid argument " + TABLE_NUMBER
                    + " for parameter source: integer value expected");
            }
        } else {
            number = DEFAULT_TABLE_NUMBER;
        }

        String range = (String) params.get(RANGE);
        if (range == null) {
            range = DEFAULT_RANGE;
        }

        String hhr = (String) params.get(HAS_HEADER_ROW);
        boolean hasHeaderRow;
        if (hhr != null) {
            hasHeaderRow = hhr.equalsIgnoreCase("true");
        } else {
            hasHeaderRow = DEFAULT_HAS_HEADER_ROW;
        }

        String hhc = (String) params.get(HAS_HEADER_COLUMN);
        boolean hasHeaderColumn;
        if (hhc != null) {
            hasHeaderColumn = hhc.equalsIgnoreCase("true");
        } else {
            hasHeaderColumn = DEFAULT_HAS_HEADER_COLUMN;
        }

        String decimal = (String) params.get(DECIMAL_SYMBOL);

        String ia = (String) params.get(IGNORE_ALPHA);
        boolean ignoreAlpha;
        if (ia != null) {
            ignoreAlpha = ia.equals("true");
        } else {
            ignoreAlpha = DEFAULT_IGNORE_ALPHA;
        }

        init(doc, number, range, hasHeaderRow, hasHeaderColumn, decimal, ignoreAlpha, context);
    }

    private void init(String docName, int tableNumber, String range, boolean hasHeaderRow, boolean hasHeaderColumn,
        String decimalSymbolSelector, boolean ignoreAlpha, XWikiContext context) throws DataSourceException
    {
        try {
            XWikiDocument doc = context.getWiki().getDocument(docName, context);
            RadeoxHelper rHelper = new RadeoxHelper(doc, context);
            Table table = rHelper.getTable(tableNumber);
            parseRange(range, hasHeaderRow, hasHeaderColumn, table);
            setDecimalSymbol(decimalSymbolSelector);
            this.ignoreAlpha = ignoreAlpha;
            makeDataMatrix(table);
            makeHeaders(table);
        } catch (NumberFormatException e) {
            throw new DataSourceException(e);
        } catch (XWikiException e) {
            throw new DataSourceException(e);
        }
    }

    private void setDecimalSymbol(String decimalSymbolSelector) throws DataSourceException
    {
        if (decimalSymbolSelector != null) {
            if (decimalSymbolSelector.equals(COMMA_SELECTOR)) {
                decimalSymbol = COMMA;
                digitGroupingSymbol = PERIOD;
            } else if (decimalSymbolSelector.equals(PERIOD_SELECTOR)) {
                decimalSymbol = PERIOD;
                digitGroupingSymbol = COMMA;
            } else {
                throw new DataSourceException("Invalid argument " + DECIMAL_SYMBOL + " for parameter source:"
                    + "comma or period expected; found: " + decimalSymbolSelector);
            }
        }
    }

    private void makeDataMatrix(Table t)
    {
        data = new Number[endRow - startRow + 1][endColumn - startColumn + 1];
        for (int y = startRow; y <= endRow; y++) {
            for (int x = startColumn; x <= endColumn; x++) {
                try {
                    data[y - startRow][x - startColumn] = toNumber(t.getXY(x, y).toString());
                } catch (Exception e) {
                    data[y - startRow][x - startColumn] = null;
                }
            }
        }
    }

    private void makeHeaders(Table t)
    {
        if (headerColumnIndex >= 0) {
            headerColumn = new String[endRow - startRow + 1];
            for (int y = startRow; y <= endRow; y++) {
                try {
                    headerColumn[y - startRow] = t.getXY(headerColumnIndex, y).toString();
                } catch (Exception e) {
                    headerColumn[y - startRow] = "";
                }
            }
        }
        if (headerRowIndex >= 0) {
            headerRow = new String[endColumn - startColumn + 1];
            for (int x = startColumn; x <= endColumn; x++) {
                try {
                    headerRow[x - startColumn] = t.getXY(x, headerRowIndex).toString();
                } catch (Exception ex) {
                    headerRow[x - startColumn] = "";
                }
            }
        }
    }

    private Number toNumber(String str) throws NumberFormatException
    {
        str = str.trim();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if ('0' <= ch && ch <= '9') {
                sb.append(ch);
            } else if (ch == decimalSymbol) {
                sb.append(".");
            } else if (ch == digitGroupingSymbol) {
                if (sb.length() > 0 && i < str.length() - 1 && '0' <= str.charAt(i - 1) && str.charAt(i - 1) <= '9'
                    && '0' <= str.charAt(i + 1) && str.charAt(i + 1) <= '9')
                {
                    // ignore digitGroupingSymbol
                } else if (ignoreAlpha) {
                    // ignore
                } else {
                    throw new NumberFormatException("Invalid character: " + ch);
                }
            } else if (ignoreAlpha) {
                // ignore
            } else {
                throw new NumberFormatException("Invalid character: " + ch);
            }
        }
        str = sb.toString();
        try {
            return new Long(str);
        } catch (NumberFormatException e1) {
            try {
                return new Double(str);
            } catch (NumberFormatException e2) {
                try {
                    new BigInteger(str);
                } catch (NumberFormatException e3) {
                    try {
                        return new BigDecimal(str);
                    } catch (NumberFormatException e4) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Valid string formats: Xm-Yn => rectangular range X-Y => whole columns range m-n => whole rows range * => enire
     * table Where X, Y are Uppercase letters, m, n are numbers First column is A, first row is 1
     */
    private void parseRange(String range, boolean hasHeaderRow, boolean hasHeaderColumn, Table t)
        throws DataSourceException
    {
        if (range.matches("[A-Z][0-9]+" + RANGE_SEP + "[A-Z][0-9]+")) {
            startColumn = range.charAt(0) - 'A';
            startRow = Integer.parseInt(range.substring(1, range.indexOf(RANGE_SEP))) - 1;
            endColumn = range.charAt(range.indexOf(RANGE_SEP) + 1) - 'A';
            endRow = Integer.parseInt(range.substring(range.indexOf(RANGE_SEP) + 2)) - 1;
        } else if (range.matches("[A-Z]" + RANGE_SEP + "[A-Z]")) {
            startColumn = range.charAt(0) - 'A';
            startRow = 0;
            endColumn = range.charAt(range.indexOf(RANGE_SEP) + 1) - 'A';
            endRow = getTableRowCount(t);
        } else if (range.matches("[0-9]+" + RANGE_SEP + "[0-9]+")) {
            startColumn = 0;
            startRow = Integer.parseInt(range.substring(0, range.indexOf(RANGE_SEP))) - 1;
            endColumn = getTableColumnCount(t);
            endRow = Integer.parseInt(range.substring(range.indexOf(RANGE_SEP) + 1)) - 1;
        } else if (range.equals("*")) {
            startColumn = 0;
            startRow = 0;
            endColumn = getTableColumnCount(t);
            endRow = getTableRowCount(t);
        }
        if (hasHeaderColumn) {
            this.headerColumnIndex = startColumn++;
            if (startColumn > endColumn) {
                throw new EmptyDataSourceException("Data source cannot contain only a header");
            }
        }
        if (hasHeaderRow) {
            this.headerRowIndex = startRow++;
            if (startRow > endRow) {
                throw new EmptyDataSourceException("Data source cannot contain only a header");
            }
        }
    }

    public static int getTableColumnCount(Table t) throws DataSourceException
    {
        int i = 0;
        try {
            while (true) {
                t.getXY(i, 0);
                i++;
            }
        } catch (Exception ex) {
            // Reached the table limit
            i--;
        }
        if (i < 0) {
            throw new EmptyDataSourceException();
        }
        return i;
    }

    public static int getTableRowCount(Table t) throws DataSourceException
    {
        int i = 0;
        try {
            while (true) {
                t.getXY(0, i);
                i++;
            }
        } catch (Exception ex) {
            // Reached the table limit
            i--;
        }
        if (i < 0) {
            throw new EmptyDataSourceException();
        }
        return i;
    }
}
