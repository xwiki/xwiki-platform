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

import org.jfree.data.general.Dataset;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.data.category.CategoryDataset;
import org.junit.jupiter.api.Test;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests building a category dataset from a table data source.
 * 
 * @version $Id$
 * @since 4.2M1
 */
@ComponentTest
class TableCategoryDatasetBuilderTest extends AbstractMacroContentTableBlockDataSourceTest
{
    @Test
    void buildCategoryDataset() throws Exception
    {
        String content =
            "| column 1 | column 2 | column 3 | column 4\n" +
            "| row 1 | 12 | 13 | 14 \n" +
            "| row 2 | 22 | 23 | 24 \n";
        setUpContentExpectation(content);

        this.source.buildDataset(content, map("type", "line", "range", "B2-D3"), null);

        ChartModel chartModel = this.source.getChartModel();

        Dataset dataset = chartModel.getDataset();

        assertTrue(dataset instanceof CategoryDataset);
        assertTrue(chartModel.getAxis(0) instanceof CategoryAxis);
        assertTrue(chartModel.getAxis(1) instanceof ValueAxis);

        CategoryDataset categoryDataset = (CategoryDataset) dataset;

        assertTrue(categoryDataset.getRowKey(0).equals(" row 1 "));
        assertTrue(categoryDataset.getRowKey(1).equals(" row 2 "));

        assertTrue(categoryDataset.getColumnKey(0).equals(" column 2 "));
        assertTrue(categoryDataset.getColumnKey(1).equals(" column 3 "));
        assertTrue(categoryDataset.getColumnKey(2).equals(" column 4"));

        assertTrue(categoryDataset.getValue(0, 0).intValue() == 12);
        assertTrue(categoryDataset.getValue(0, 1).intValue() == 13);
        assertTrue(categoryDataset.getValue(0, 2).intValue() == 14);
        assertTrue(categoryDataset.getValue(1, 0).intValue() == 22);
        assertTrue(categoryDataset.getValue(1, 1).intValue() == 23);
        assertTrue(categoryDataset.getValue(1, 2).intValue() == 24);
    }

    @Test
    void buildCategoryDatasetColumnsSeries() throws Exception
    {
        String content =
            "| column 1 | column 2 | column 3 | column 4\n" +
            "| row 1 | 12 | 13 | 14 \n" +
            "| row 2 | 22 | 23 | 24 \n";
        setUpContentExpectation(content);

        this.source.buildDataset(content, map("type", "line", "range", "B2-D3", "series", "columns"), null);

        ChartModel chartModel = this.source.getChartModel();

        Dataset dataset = chartModel.getDataset();

        assertTrue(dataset instanceof CategoryDataset);
        assertTrue(chartModel.getAxis(0) instanceof CategoryAxis);
        assertTrue(chartModel.getAxis(1) instanceof ValueAxis);

        CategoryDataset categoryDataset = (CategoryDataset) dataset;

        assertTrue(categoryDataset.getColumnKey(0).equals(" row 1 "));
        assertTrue(categoryDataset.getColumnKey(1).equals(" row 2 "));

        assertTrue(categoryDataset.getRowKey(0).equals(" column 2 "));
        assertTrue(categoryDataset.getRowKey(1).equals(" column 3 "));
        assertTrue(categoryDataset.getRowKey(2).equals(" column 4"));

        assertTrue(categoryDataset.getValue(0, 0).intValue() == 12);
        assertTrue(categoryDataset.getValue(1, 0).intValue() == 13);
        assertTrue(categoryDataset.getValue(2, 0).intValue() == 14);
        assertTrue(categoryDataset.getValue(0, 1).intValue() == 22);
        assertTrue(categoryDataset.getValue(1, 1).intValue() == 23);
        assertTrue(categoryDataset.getValue(2, 1).intValue() == 24);
    }
}
