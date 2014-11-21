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

import java.util.Map;

import org.jfree.data.general.Dataset;
import org.xwiki.rendering.internal.macro.chart.source.LocaleConfiguration;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * The jfree chart library generates charts from different classes of datasets.  This is a common interface for classes
 * that implements building datasets from table data sources.
 *
 * Please, note the distinction between a data source and a dataset.  A data source is something from which data is
 * extracted, while a dataset is an encapsulation of data that can be passed to the chart library.
 *
 * @version $Id$
 * @since 4.2M1
 */
public interface TableDatasetBuilder
{
    /**
     * Set the number of rows in the table.
     *
     * @param numberOfRows the number of rows.
     */
    void setNumberOfRows(int numberOfRows);

    /**
     * Set the number of columns in the table.
     *
     * @param numberOfColumns the number of columns.
     */
    void setNumberOfColumns(int numberOfColumns);

    /**
     * Set the column heading at the given column index.
     *
     * @param columnIndex The column index.
     * @param heading The heading text.
     * @throws MacroExecutionException if the heading is invalid for the dataset.
     */
    void setColumnHeading(int columnIndex, String heading) throws MacroExecutionException;

    /**
     * Set the row heading at the given column index.
     *
     * @param rowIndex The row index.
     * @param heading The heading text.
     * @throws MacroExecutionException if the heading is invalid for the dataset.
     */
    void setRowHeading(int rowIndex, String heading) throws MacroExecutionException;

    /**
     * Set a value in the dataset.
     *
     * @param rowIndex The row index.
     * @param columnIndex The column index.
     * @param value The value.
     * @throws MacroExecutionException if the value is invalid for the dataset.
     */
    void setValue(int rowIndex, int columnIndex, Number value) throws MacroExecutionException;

    /**
     * Set wether the table should be transposed.
     *
     * @param transpose Indicates that the table shoudl be transposed.
     */
    void setTranspose(boolean transpose);

    /**
     * @return the built dataset.
     */
    Dataset getDataset();

    /**
     * Configure the dataset builder from the parameters.
     *
     * @param parameters The parameters.
     * @throws MacroExecutionException if the parameters are invalid for this builder.
     */
    void setParameters(Map<String, String> parameters) throws MacroExecutionException;

    /**
     * @return indication of whether column headings should be forced.  I.e., if the first column must be headings even
     *         if included in the range.
     */
    boolean forceColumnHeadings();

    /**
     * @return indication of whether row headings should be forced.  I.e., if the first row must be headings even
     *         if included in the range.
     */
    boolean forceRowHeadings();

    /**
     * Set the locale configuration for the builders that needs it.
     *
     * @param localeConfiguration The locale configuration
     */
    void setLocaleConfiguration(LocaleConfiguration localeConfiguration);
}
