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
package org.xwiki.chart.internal.plot;

import java.util.Map;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.xwiki.chart.model.ChartModel;

/**
 * Generate Plots for Category data sets.
 *
 * @version $Id$
 * @since 4.1M1
 */
public abstract class AbstractCategoryPlotGenerator implements PlotGenerator
{
    @Override
    public Plot generate(ChartModel model, Map<String, String> parameters)
    {
        CategoryAxis domainAxis = new CategoryAxis();
        ValueAxis rangeAxis = new NumberAxis();
        return new CategoryPlot(buildCategoryDataset(model, parameters), domainAxis, rangeAxis, getRenderer());
    }

    /**
     * @return an {@link CategoryItemRenderer} to be used for plotting the chart.
     */
    protected abstract CategoryItemRenderer getRenderer();

    /**
     * Builds a new {@link org.jfree.data.category.DefaultCategoryDataset} corresponding to the provided {@link ChartModel}.
     *
     * @param model the {@link ChartModel} instance.
     * @param parameters additional parameters.
     * @return a {@link org.jfree.data.category.DefaultCategoryDataset} corresponding to the provided {@link ChartModel}.
     */
    public DefaultCategoryDataset buildCategoryDataset(ChartModel model, Map<String, String> parameters)
    {
        String dataSeries = parameters.get("series");
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        if ("rows".equals(dataSeries)) {
            extractRows(model, dataset);
        } else {
            extractColumns(model, dataset);
        }
        return dataset;
    }

    /**
     * Extracts data rows from the {@link ChartModel} provided and populates the {@link DefaultCategoryDataset}
     * accordingly.
     *
     * @param model the {@link ChartModel} instance.
     * @param dataset the {@link DefaultCategoryDataset} to be populated.
     */
    private void extractRows(ChartModel model, DefaultCategoryDataset dataset)
    {
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int column = 0; column < model.getColumnCount(); column++) {
                dataset.addValue(model.getCellValue(row, column), model.getRowHeader(row), model
                    .getColumnHeader(column));
            }
        }
    }

    /**
     * Extracts data columns from the {@link ChartModel} provided and populates the {@link DefaultCategoryDataset}
     * accordingly.
     *
     * @param model the {@link ChartModel} instance.
     * @param dataset the {@link DefaultCategoryDataset} to be populated.
     */
    private void extractColumns(ChartModel model, DefaultCategoryDataset dataset)
    {
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int column = 0; column < model.getColumnCount(); column++) {
                dataset.addValue(model.getCellValue(row, column), model.getColumnHeader(column), model
                    .getRowHeader(row));
            }
        }
    }
}
