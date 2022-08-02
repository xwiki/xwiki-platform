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
import org.jfree.data.general.PieDataset;
import org.junit.jupiter.api.Test;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests building a pie dataset from a table data source.
 * 
 * @version $Id$
 * @since 4.2M1
 */
@ComponentTest
class TablePieDatasetBuilderTest extends AbstractMacroContentTableBlockDataSourceTest
{
    @Test
    void buildPieDataset() throws Exception
    {
        String content =
            "| column 1 | column 2 | column 3 | column 4\n" +
            "| row 1 | 12 | 13 | 14 \n" +
            "| row 2 | 22 | 23 | 24 \n";
        setUpContentExpectation(content);

        this.source.buildDataset(content, map("type", "pie", "range", "B2-D3"), null);

        ChartModel chartModel = this.source.getChartModel();

        Dataset dataset = chartModel.getDataset();

        assertTrue(dataset instanceof PieDataset);

        PieDataset pieDataset = (PieDataset) dataset;

        assertTrue(pieDataset.getKey(0).equals(" row 1 "));
        assertTrue(pieDataset.getKey(1).equals(" row 2 "));

        assertTrue(pieDataset.getValue(0).intValue() == 12);
        assertTrue(pieDataset.getValue(1).intValue() == 22);
    }

    @Test
    void buildPieDatasetRowsSeries() throws Exception
    {
        String content =
            "| column 1 | column 2 | column 3 | column 4\n" +
            "| row 1 | 12 | 13 | 14 \n" +
            "| row 2 | 22 | 23 | 24 \n";
        setUpContentExpectation(content);

        this.source.buildDataset(content, map("type", "pie", "range", "B2-D3", "series", "rows"), null);

        ChartModel chartModel = this.source.getChartModel();

        Dataset dataset = chartModel.getDataset();

        assertTrue(dataset instanceof PieDataset);

        PieDataset pieDataset = (PieDataset) dataset;

        assertTrue(pieDataset.getKey(0).equals(" column 2 "));
        assertTrue(pieDataset.getKey(1).equals(" column 3 "));
        assertTrue(pieDataset.getKey(2).equals(" column 4"));

        assertTrue(pieDataset.getValue(0).intValue() == 12);
        assertTrue(pieDataset.getValue(1).intValue() == 13);
        assertTrue(pieDataset.getValue(2).intValue() == 14);
    }
}
