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
package org.xwiki.rendering.internal.macro.chart.source.table;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.chart.dataset.DatasetType;
import org.xwiki.rendering.block.TableBlock;
import org.xwiki.rendering.block.TableCellBlock;
import org.xwiki.rendering.block.TableRowBlock;
import org.xwiki.rendering.internal.macro.chart.source.AbstractDataSource;
import org.xwiki.rendering.internal.macro.chart.source.SimpleChartModel;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Data source that extracts values from a table (in any syntax that supports tables).  For example in xwiki/2.1 syntax:
 *
 * <p>
 * <code><pre>
 * |=           |= column label 1 |= column label 2  
 * | row label 1| 11              |  12
 * | row label 2| 21              |  22
 * </pre></code>
 *
 * @version $Id$
 * @since 4.2M1
 */
public abstract class AbstractTableBlockDataSource extends AbstractDataSource
{
    /**
     * The number of letters that can be used in the column identifier.
     */
    private static final int LETTER_RANGE_LENGTH = 'Z' - 'A' + 1;

    /**
     * Identifies the data range to be used for plotting.
     */
    private static final String RANGE_PARAM = "range";

    /**
     * Indicates how the table values should be mapped to the dataset.
     */
    private static final String SERIES_PARAM = "series";

    /**
     * The columns value for the series parameter.
     */
    private static final String SERIES_COLUMNS = "columns";

    /**
     * The rows value for the series parameter.
     */
    private static final String SERIES_ROWS = "rows";

    /**
     * No-limit indicator symbol in ranges.
     */
    private static final String NO_LIMIT_SYMBOL = ".";

    /**
     * Pattern matching the cell range.
     */
    private static final Pattern RANGE_PATTERN =
        Pattern.compile("^([A-Z]+|\\.)([0-9]+|\\.)-([A-Z]+|\\.)([0-9]+|\\.)$");

    /**
     * The name of the category dataset.
     */
    private static final String CATEGORY_DATASET = "category";

    /**
     * The name of the time series dataset.
     */
    private static final String TIME_SERIES_DATASET = "timeseries";

    /**
     * The name of the pie dataset.
     */
    private static final String PIE_DATASET = "pie";

    /**
     * The default dataset.
     */
    private static final String DEFAULT_DATASET = CATEGORY_DATASET;

    /**
     * The range parameter.
     */
    private String range;

    /**
     * The series parameter.
     */
    private String series;

    /**
     * Used to convert cell blocks in plain text so that it can be converted to numbers.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    @Override
    public void buildDataset(String macroContent, Map<String, String> parameters, MacroTransformationContext context)
        throws MacroExecutionException
    {
        validateParameters(parameters);

        TableBlock tableBlock = getTableBlock(macroContent, context);

        int[] dataRange = getDataRange(tableBlock);

        TableDatasetBuilder datasetBuilder;
        setChartModel(new SimpleChartModel());

        switch (getDatasetType()) {
            case CATEGORY:
                datasetBuilder = new TableCategoryDatasetBuilder();
                break;
            case PIE:
                datasetBuilder = new TablePieDatasetBuilder();
                break;
            case TIMETABLE_XY:
                datasetBuilder = new TableTimeTableXYDatasetBuilder();
                break;
            default:
                throw new MacroExecutionException(String.format("Unsupported dataset type [%s]",
                    getDatasetType().getName()));
        }

        setAxes();

        datasetBuilder.setLocaleConfiguration(getLocaleConfiguration());
        datasetBuilder.setParameters(parameters);

        if (SERIES_COLUMNS.equals(series)) {
            datasetBuilder.setTranspose(true);
        }

        buildDataset(tableBlock, dataRange, datasetBuilder);

        setDataset(datasetBuilder.getDataset());
    }

    /**
     * @param tableBlock The table block to parse.
     * @param startRow The first row to include.
     * @param endRow The last row to include.
     * @param startColumn The first column to include.
     * @param datasetBuilder The dataset builder.
     * @throws MacroExecutionException if there are any errors in the table.
     */
    private void getRowKeys(TableBlock tableBlock, int startRow, int endRow, int startColumn,
        TableDatasetBuilder datasetBuilder) throws MacroExecutionException
    {

        datasetBuilder.setNumberOfRows(endRow - startRow + 1);

        if (startColumn > 0) {
            Set<String> rowKeySet = new HashSet<String>();
            for (int i = startRow; i <= endRow; i++) {
                TableRowBlock tableRow = (TableRowBlock) tableBlock.getChildren().get(i);
                String key = cellContentAsString((TableCellBlock) tableRow.getChildren().get(startColumn - 1));
                datasetBuilder.setRowHeading(i - startRow, key);
            }
        } else {
            for (int i = startRow; i <= endRow; i++) {
                datasetBuilder.setRowHeading(i - startRow, "R" + i);
            }
        }
    }

    /**
     * @param tableBlock The table block to parse.
     * @param startColumn The first column to include.
     * @param endColumn The last column to include.
     * @param startRow The first row to include.
     * @param datasetBuilder The dataset builder.
     * @throws MacroExecutionException if there are any errors in the table.
     */
    private void getColumnKeys(TableBlock tableBlock, int startColumn, int endColumn, int startRow,
        TableDatasetBuilder datasetBuilder)
        throws MacroExecutionException
    {
        datasetBuilder.setNumberOfColumns(endColumn - startColumn + 1);

        if (startRow > 0) {
            TableRowBlock tableRow = (TableRowBlock) tableBlock.getChildren().get(startRow - 1);
            for (int i = startColumn; i <= endColumn; i++) {
                String key = cellContentAsString((TableCellBlock) tableRow.getChildren().get(i));
                datasetBuilder.setColumnHeading(i - startColumn, key);
            }
        } else {
            for (int i = startColumn; i <= endColumn; i++) {
                datasetBuilder.setColumnHeading(i - startColumn, "C" + i);
            }
        }
    }

    /**
     * Build a category dataset.
     *
     * @param tableBlock The table block to parse.
     * @param dataRange The data range.
     * @param datasetBuilder The dataset builder.
     * @throws MacroExecutionException if there are any errors.
     */
    private void buildDataset(TableBlock tableBlock, int[] dataRange,
        TableDatasetBuilder datasetBuilder) throws MacroExecutionException
    {
        int startRow = dataRange[0];
        int startColumn = dataRange[1];
        int endRow = dataRange[2];
        int endColumn = dataRange[3];

        if (startRow == 0 && datasetBuilder.forceRowHeadings()) {
            startRow = 1;
        }

        if (startColumn == 0 && datasetBuilder.forceColumnHeadings()) {
            startColumn = 1;
        }

        getRowKeys(tableBlock, startRow, endRow, startColumn, datasetBuilder);

        getColumnKeys(tableBlock, startColumn, endColumn, startRow, datasetBuilder);

        for (int i = startRow; i <= endRow; i++) {
            if (i < tableBlock.getChildren().size()) {
                TableRowBlock tableRow = (TableRowBlock) tableBlock.getChildren().get(i);
                for (int j = startColumn; j <= endColumn; j++) {
                    if (j < tableRow.getChildren().size()) {
                        Number value = cellContentAsNumber((TableCellBlock) tableRow.getChildren().get(j));
                        datasetBuilder.setValue(i - startRow, j - startColumn, value);
                    } else {
                        throw new MacroExecutionException("Data range (columns) overflow.");
                    }
                }
            } else {
                throw new MacroExecutionException("Data range (rows) overflow.");
            }
        }
    }

    /**
     * Converts a column identifier ('G') into a column index number (6).
     *
     * @param identifier the cell identifier, containing a column ([A-Z]{1,2})
     * @return the column number extracted from the identifier
     */
    protected static Integer getColumnNumberFromIdentifier(String identifier)
    {
        if (NO_LIMIT_SYMBOL.equals(identifier)) {
            return null;
        }
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

    /**
     * Converts a row identifier ('6') into a column index number (6).
     *
     * @param identifier the cell identifier, containing a column.
     * @return the column number extracted from the identifier
     */
    private static Integer getRowNumberFromIdentifier(String identifier)
    {
        if (NO_LIMIT_SYMBOL.equals(identifier)) {
            return null;
        }
        return Integer.parseInt(identifier) - 1;
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
            return NumberUtils.createNumber(StringUtils.trim(stringContent));
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
     * Parses the range parameter and return an array on the form [startRow, startColumn, endRow, endColumn].  Any
     * element in the array may be {@code null} to indicate no bound.
     *
     * @return An array as described above.
     * @throws MacroExecutionException if the range parameter cannot be parsed.
     */
    private Integer[] getDataRangeFromParameter()
        throws MacroExecutionException
    {
        Integer startColumn = null;
        Integer endColumn = null;
        Integer startRow = null;
        Integer endRow = null;

        if (range != null) {

            Matcher m = RANGE_PATTERN.matcher(range);

            if (!m.matches()) {
                throw new MacroExecutionException(String.format("Invalid range specification: [%s].", range));
            }

            startColumn = getColumnNumberFromIdentifier(m.group(1));
            startRow = getRowNumberFromIdentifier(m.group(2));
            endColumn = getColumnNumberFromIdentifier(m.group(3));
            endRow = getRowNumberFromIdentifier(m.group(4));

            if (startColumn != null && endColumn != null && startColumn > endColumn) {
                throw new MacroExecutionException(
                    String.format("Invalid data range, end column mustn't come before start column: [%s].", range));
            }

            if (startRow != null && endRow != null && startRow > endRow) {
                throw new MacroExecutionException(
                    String.format("Invalid data range, end row mustn't come before start row: [%s].", range));
            }
        }

        return new Integer[]{startRow, startColumn, endRow, endColumn};
    }

    /**
     * Calculates the data-range that is to be used for plotting the chart.
     *
     * @param tableBlock the {@link TableBlock}.
     * @return an integer array consisting of start-row, start-column, end-row and end-column of the data range.
     * @throws MacroExecutionException if it's not possible to determine the data range correctly.
     */
    protected int[] getDataRange(TableBlock tableBlock)
        throws MacroExecutionException
    {

        Integer[] r = getDataRangeFromParameter();

        int rowCount = tableBlock.getChildren().size();
        if (rowCount > 0) {
            TableRowBlock firstRow = (TableRowBlock) tableBlock.getChildren().get(0);
            int columnCount = firstRow.getChildren().size();
            if (columnCount > 0) {
                return new int[]{r[0] != null ? r[0] : 0,
                    r[1] != null ? r[1] : 0,
                    r[2] != null ? r[2] : rowCount - 1,
                    r[3] != null ? r[3] : columnCount - 1};
            }
        }

        throw new MacroExecutionException("Data table is incomplete.");
    }

    @Override
    public void validateDatasetType() throws MacroExecutionException
    {
        super.validateDatasetType();

        switch (getDatasetType()) {
            case CATEGORY:
                break;
            case PIE:
                break;
            case TIMETABLE_XY:
                break;
            default:
                throw new MacroExecutionException(
                    String.format("Dataset type [%s] is not supported by the table data source.",
                        getDatasetType().getName()));
        }
    }

    /**
     * Returns the {@link TableBlock} which contains the data to be plotted.
     *
     * @param macroContent macro content.
     * @param context the macro transformation context, used for example to find out the current document reference
     * @return the {@link TableBlock} containing the data to be plotted.
     * @throws MacroExecutionException if it's not possible to locate the {@link TableBlock} specified by the user.
     */
    protected abstract TableBlock getTableBlock(String macroContent, MacroTransformationContext context)
        throws MacroExecutionException;

    @Override
    protected boolean setParameter(String key, String value) throws MacroExecutionException
    {
        if (RANGE_PARAM.equals(key)) {
            range = value;
            return true;
        }
        if (SERIES_PARAM.equals(key)) {
            series = value;
            return true;
        }
        return false;
    }

    @Override
    protected void validateParameters() throws MacroExecutionException
    {
        if (series == null) {
            // Backwards compliancy: The "series" parameter doesn't really make sense for category datasets, but setting
            // it to "columns" indicates that the table should be transposed.  Adding to oddities, it also takes on
            // different default values depending on the dataset type.
            series = getDatasetType() == DatasetType.CATEGORY ? SERIES_ROWS : SERIES_COLUMNS;
        } else {
            if (!(SERIES_COLUMNS.equals(series) || SERIES_ROWS.equals(series))) {
                throw new MacroExecutionException(String.format("Unsupported value for parameter [%s]: [%s]",
                    SERIES_PARAM, series));
            }
        }
    }
}
