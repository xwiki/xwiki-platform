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

import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.xwiki.rendering.internal.macro.chart.source.LocaleConfiguration;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * A builder of category datasets ({@see org.jfree.data.category.CategoryDataset}) from table data sources.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class TableCategoryDatasetBuilder implements TableDatasetBuilder
{
    /**
     * The catetegory dataset.
     */
    private final DefaultCategoryDataset dataset = new DefaultCategoryDataset();

    /**
     * The array of row keys.
     */
    private String[] rowKeys;

    /**
     * The array of column keys.
     */
    private String[] columnKeys;

    /**
     * Indicates if the transpose of the table should be used.
     */
    private boolean transpose;

    /**
     * Row key set used for validating the absense of duplicate row keys.
     */
    private final Set<String> rowKeySet = new HashSet<String>();

    /**
     * Column key set used for validating the absense of duplicate column keys.
     */
    private final Set<String> columnKeySet = new HashSet<String>();

    @Override
    public void setNumberOfRows(int numberOfRows)
    {
        rowKeys = new String[numberOfRows];
    }

    @Override
    public void setNumberOfColumns(int numberOfColumns)
    {
        columnKeys = new String[numberOfColumns];
    }

    @Override
    public void setTranspose(boolean transpose)
    {
        this.transpose = transpose;
    }

    @Override
    public void setColumnHeading(int columnIndex, String heading) throws MacroExecutionException
    {
        if (columnKeySet.contains(heading)) {
            throw new MacroExecutionException(String.format("Duplicate column keys in table: [%s]", heading));
        }
        columnKeySet.add(heading);
        columnKeys[columnIndex] = heading;
    }

    @Override
    public void setRowHeading(int rowIndex, String heading) throws MacroExecutionException
    {
        if (rowKeySet.contains(heading)) {
            throw new MacroExecutionException(String.format("Duplicate row keys in table: [%s]", heading));
        }
        rowKeySet.add(heading);
        rowKeys[rowIndex] = heading;
    }

    @Override
    public void setValue(int rowIndex, int columnIndex, Number value)
    {
        if (transpose) {
            dataset.addValue(value, columnKeys[columnIndex], rowKeys[rowIndex]);
        } else {
            dataset.addValue(value, rowKeys[rowIndex], columnKeys[columnIndex]);
        }
    }

    @Override
    public Dataset getDataset()
    {
        return dataset;
    }

    @Override
    public void setParameters(Map<String, String> parameters)
    {
    }

    @Override
    public boolean forceColumnHeadings()
    {
        return false;
    }

    @Override
    public boolean forceRowHeadings()
    {
        return false;
    }

    @Override
    public void setLocaleConfiguration(LocaleConfiguration localeConfiguration)
    {
    }
}
