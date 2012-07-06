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

import org.junit.Assert;
import org.junit.Test;

import org.jfree.data.general.Dataset;
import org.jfree.data.general.PieDataset;

import org.xwiki.chart.model.ChartModel;

/**
 * Tests building a pie dataset from a table data source.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class TablePieDatasetBuilderTest extends AbstractMacroContentTableBlockDataSourceTest
{
    @Test
    public void testBuildPieDataset() throws Exception
    {
        String content =
            "| column 1 | column 2 | column 3 | column 4\n" +
            "| row 1 | 12 | 13 | 14 \n" +
            "| row 2 | 22 | 23 | 24 \n";
        setUpContentExpectation(content);

        getDataSource().buildDataset(content, map("type", "pie", "range", "B2-D3"), null);

        ChartModel chartModel = getDataSource().getChartModel();

        Dataset dataset = chartModel.getDataset();

        Assert.assertTrue(dataset instanceof PieDataset);

        PieDataset pieDataset = (PieDataset) dataset;

        Assert.assertTrue(pieDataset.getKey(0).equals(" row 1 "));
        Assert.assertTrue(pieDataset.getKey(1).equals(" row 2 "));

        Assert.assertTrue(pieDataset.getValue(0).intValue() == 12);
        Assert.assertTrue(pieDataset.getValue(1).intValue() == 22);
    }

    @Test
    public void testBuildPieDatasetRowsSeries() throws Exception
    {
        String content =
            "| column 1 | column 2 | column 3 | column 4\n" +
            "| row 1 | 12 | 13 | 14 \n" +
            "| row 2 | 22 | 23 | 24 \n";
        setUpContentExpectation(content);

        getDataSource().buildDataset(content, map("type", "pie", "range", "B2-D3", "series", "rows"), null);

        ChartModel chartModel = getDataSource().getChartModel();

        Dataset dataset = chartModel.getDataset();

        Assert.assertTrue(dataset instanceof PieDataset);

        PieDataset pieDataset = (PieDataset) dataset;

        Assert.assertTrue(pieDataset.getKey(0).equals(" column 2 "));
        Assert.assertTrue(pieDataset.getKey(1).equals(" column 3 "));
        Assert.assertTrue(pieDataset.getKey(2).equals(" column 4"));

        Assert.assertTrue(pieDataset.getValue(0).intValue() == 12);
        Assert.assertTrue(pieDataset.getValue(1).intValue() == 13);
        Assert.assertTrue(pieDataset.getValue(2).intValue() == 14);
    }
}
