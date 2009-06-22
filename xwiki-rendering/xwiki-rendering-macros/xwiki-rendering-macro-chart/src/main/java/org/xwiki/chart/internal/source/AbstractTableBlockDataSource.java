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

import org.xwiki.chart.model.ChartModel;
import org.xwiki.chart.model.DefaultChartModel;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.chart.ChartDataSource;
import org.xwiki.rendering.renderer.XWikiSyntaxRenderer;
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
    /**
     * Identifies the data range to be used for plotting.
     */
    private static final String RANGE_PARAM = "range";

    /**
     * String separator between range-cell & stop-cell.
     */
    private static final String RANGE_SEPERATOR = "-";

    /**
     * Regex for specifying a table cell. (for range)
     */
    private static final String RANGE_PATTERN = "[A-Z][0-9]+";

    /**
     * {@inheritDoc}
     */
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
     * Parses the cell content as a string.
     * 
     * @param cell the {@link TableCellBlock}.
     * @return cell content parsed as a string.
     */
    private String cellContentAsString(TableCellBlock cell)
    {
        XDOM cellContent = new XDOM(cell.getChildren());
        WikiPrinter printer = new DefaultWikiPrinter();
        XWikiSyntaxRenderer renderer = new XWikiSyntaxRenderer(printer);
        cellContent.traverse(renderer);
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
            if (!range.matches(RANGE_PATTERN + RANGE_SEPERATOR + RANGE_PATTERN)) {
                throw new MacroExecutionException(String.format("Invalid range specification: [%s].", range));
            }

            int startRow = 0;
            int startColumn = 0;
            int endRow = 0;
            int endColumn = 0;

            String[] rangeSegments = range.split(RANGE_SEPERATOR);
            startColumn = rangeSegments[0].charAt(0) - 'A';
            endColumn = rangeSegments[1].charAt(0) - 'A';
            startRow = Integer.parseInt(rangeSegments[0].substring(1)) - 1;
            endRow = Integer.parseInt(rangeSegments[1].substring(1)) - 1;

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
