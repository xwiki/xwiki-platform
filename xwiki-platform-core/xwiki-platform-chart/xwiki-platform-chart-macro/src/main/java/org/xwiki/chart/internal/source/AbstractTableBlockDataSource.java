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
package org.xwiki.chart.internal.source;

import java.util.Map;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.chart.model.ChartModel;
import org.xwiki.chart.model.DefaultChartModel;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.chart.ChartDataSource;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;

/**
 * Super class for {@link TableBlock} based {@link ChartDataSource} implementations.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractTableBlockDataSource implements ChartDataSource
{
    /** The number of letters that can be used in the column identifier. */
    private static final int LETTER_RANGE_LENGTH = 'Z' - 'A' + 1;

    /**
     * Identifies the data range to be used for plotting.
     */
    private static final String RANGE_PARAM = "range";

    /**
     * String separator between range-cell & stop-cell.
     */
    private static final String RANGE_SEPERATOR = "-";

    /** Regex matching a table column (for ranges). */
    private static final String RANGE_COLUMN_REGEX = "[A-Z]{1,2}";

    /** Regex matching a table row (for ranges). */
    private static final String RANGE_ROW_REGEX = "[0-9]+";

    /** Regex matching a table cell (for ranges). */
    private static final String RANGE_CELL_REGEX = RANGE_COLUMN_REGEX + RANGE_ROW_REGEX;

    /** Pattern matching the range separator, useful for splitting the range string. */
    private static final Pattern RANGE_SEPERATOR_PATTERN = Pattern.compile(RANGE_SEPERATOR);

    /** Pattern matching the column in a cell identifier. */
    private static final Pattern RANGE_COLUMN_PATTERN = Pattern.compile(RANGE_COLUMN_REGEX);

    /** Pattern matching the cell range. Useful for validating the range parameter. */
    private static final Pattern RANGE_PATTERN =
        Pattern.compile("^" + RANGE_CELL_REGEX + RANGE_SEPERATOR + RANGE_CELL_REGEX);

    /**
     * Used to convert cell blocks in plain text so that it can be converted to numbers.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    /**
     * Converts a column identifier ('G4') into a column index number (6).
     * 
     * @param identifier the cell identifier, containing a column ([A-Z]{1,2}) and a row ([0-9]+) specifier
     * @return the column number extracted from the identifier
     */
    public static int getColumnNumberFromIdentifier(String identifier)
    {
        int i = 0;
        int result = -1;
        char j;
        while (i < identifier.length()) {
            j = identifier.charAt(i++);
            if (!Character.isUpperCase(j)) {
                break;
            }
            result = (result + 1) * LETTER_RANGE_LENGTH + j - 'A';
        }
        return result;
    }

    @Override
    public ChartModel buildModel(String macroContent, Map<String, String> macroParameters)
        throws MacroExecutionException
    {
        populateSourceParameters(macroParameters);
        TableBlock tableBlock = getTableBlock(macroContent, macroParameters);
        int[] dataRange = getDataRange(tableBlock, macroContent, macroParameters);

        int startRow = dataRange[0];
        int startColumn = dataRange[1];
        int endRow = dataRange[2];
        int endColumn = dataRange[3];

        Number[][] data = new Number[(endRow - startRow) + 1][(endColumn - startColumn) + 1];

        for (int i = startRow; i <= endRow; i++) {
            if (i < tableBlock.getChildren().size()) {
                TableRowBlock tableRow = (TableRowBlock) tableBlock.getChildren().get(i);
                for (int j = startColumn; j <= endColumn; j++) {
                    if (j < tableRow.getChildren().size()) {
                        data[i - startRow][j - startColumn] =
                            cellContentAsNumber((TableCellBlock) tableRow.getChildren().get(j));
                    } else {
                        throw new MacroExecutionException("Data range (columns) overflow.");
                    }
                }
            } else {
                throw new MacroExecutionException("Data range (rows) overflow.");
            }
        }

        String[] rowHeaders = null;
        if (startColumn > 0) {
            rowHeaders = new String[(endRow - startRow) + 1];
            for (int i = startRow; i <= endRow; i++) {
                TableRowBlock tableRow = (TableRowBlock) tableBlock.getChildren().get(i);
                rowHeaders[i - startRow] =
                    cellContentAsString((TableCellBlock) tableRow.getChildren().get(startColumn - 1));
            }
        }

        String[] columnHeaders = null;
        if (startRow > 0) {
            columnHeaders = new String[(endColumn - startColumn) + 1];
            TableRowBlock tableRow = (TableRowBlock) tableBlock.getChildren().get(startRow - 1);
            for (int j = startColumn; j <= endColumn; j++) {
                columnHeaders[j - startColumn] = cellContentAsString((TableCellBlock) tableRow.getChildren().get(j));
            }
        }

        return new DefaultChartModel(data, rowHeaders, columnHeaders);
    }

    /**
     * Tries to parse the cell content as a number.
     * 
     * @param cell the {@link TableCellBlock}.
     * @return cell content parsed as a {@link Number}.
     * @throws MacroExecutionException if the cell does not represent a number.
     */
    private Number cellContentAsNumber(TableCellBlock cell) throws MacroExecutionException
    {
        String stringContent = cellContentAsString(cell);
        try {
            return Double.valueOf(stringContent);
        } catch (NumberFormatException ex) {
            throw new MacroExecutionException(String.format("Invalid number: [%s].", stringContent));
        }
    }

    /**
     * Renders the cell content as a string.
     * 
     * @param cell the {@link TableCellBlock}.
     * @return cell content rendered as a string.
     */
    private String cellContentAsString(TableCellBlock cell)
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        this.plainTextBlockRenderer.render(cell.getChildren(), printer);
        return printer.toString();
    }

    /**
     * If there are data-source specific parameters; populates them inside macro parameters.
     * 
     * @param macroParameters macro parameters.
     */
    protected void populateSourceParameters(Map<String, String> macroParameters)
    {
        String extraParams = macroParameters.get(PARAMS);
        if (null != extraParams) {
            String[] segments = extraParams.split(";");
            for (String segment : segments) {
                String[] keyValue = segment.split(":");
                if (keyValue.length == 2) {
                    macroParameters.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }

    /**
     * Calculates the data-range that is to be used for plotting the chart.
     * 
     * @param tableBlock the {@link TableBlock}.
     * @param macroContent macro body.
     * @param macroParameters macro parameters.
     * @return an integer array consisting of start-row, start-column, end-row and end-column of the data range.
     * @throws MacroExecutionException if it's not possible to determine the data range correctly.
     */
    protected int[] getDataRange(TableBlock tableBlock, String macroContent, Map<String, String> macroParameters)
        throws MacroExecutionException
    {
        String range = macroParameters.get(RANGE_PARAM);
        if (range != null) {

            if (!RANGE_PATTERN.matcher(range).matches()) {
                throw new MacroExecutionException(String.format("Invalid range specification: [%s].", range));
            }

            int startRow;
            int startColumn;
            int endRow;
            int endColumn;

            String[] rangeSegments = RANGE_SEPERATOR_PATTERN.split(range);

            startColumn = getColumnNumberFromIdentifier(rangeSegments[0]);
            endColumn = getColumnNumberFromIdentifier(rangeSegments[1]);
            startRow = Integer.parseInt(RANGE_COLUMN_PATTERN.matcher(rangeSegments[0]).replaceFirst("")) - 1;
            endRow = Integer.parseInt(RANGE_COLUMN_PATTERN.matcher(rangeSegments[1]).replaceFirst("")) - 1;

            if (startColumn > endColumn || startRow > endRow) {
                throw new MacroExecutionException(String.format("Invalid data range: [%s].", range));
            }

            return new int[] {startRow, startColumn, endRow, endColumn};
        }

        int rowCount = tableBlock.getChildren().size();
        if (rowCount > 0) {
            TableRowBlock firstRow = (TableRowBlock) tableBlock.getChildren().get(0);
            int columnCount = firstRow.getChildren().size();
            if (columnCount > 0) {
                return new int[] {0, 0, rowCount - 1, columnCount - 1};
            }
        }

        throw new MacroExecutionException("Data table is incomplete.");
    }

    /**
     * Returns the {@link TableBlock} which contains the data to be plotted.
     * 
     * @param macroContent macro content.
     * @param macroParameters macro parameters.
     * @return the {@link TableBlock} containing the data to be plotted.
     * @throws MacroExecutionException if it's not possible to locate the {@link TableBlock} specified by the user.
     */
    protected abstract TableBlock getTableBlock(String macroContent, Map<String, String> macroParameters)
        throws MacroExecutionException;
}
