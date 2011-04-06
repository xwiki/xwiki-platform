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
 *
 */
package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class CategoryPlotFactory
{
    private static CategoryPlotFactory uniqueInstance = new CategoryPlotFactory();

    /**
     * This class is not used directly but through area, bar and line (there is a sort of dynamic inheritance here)
     */
    private CategoryPlotFactory()
    {
        // empty
    }

    public static CategoryPlotFactory getInstance()
    {
        return uniqueInstance;
    }

    public Plot create(DataSource dataSource, CategoryItemRenderer renderer, ChartParams params)
        throws GenerateException, DataSourceException
    {
        String dataSeries = params.getString(ChartParams.SERIES);

        CategoryAxis domainAxis = new CategoryAxis();
        ValueAxis rangeAxis = new NumberAxis();
        ChartCustomizer.customizeCategoryAxis(domainAxis, params, ChartParams.AXIS_DOMAIN_PREFIX);
        ChartCustomizer.customizeValueAxis(rangeAxis, params, ChartParams.AXIS_RANGE_PREFIX);

        Class rendererClass = params.getClass(ChartParams.RENDERER);

        ChartCustomizer.customizeCategoryItemRenderer(renderer, params);

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        if ("columns".equals(dataSeries)) {
            for (int row = 0; row < dataSource.getRowCount(); row++) {
                for (int column = 0; column < dataSource.getColumnCount(); column++) {
                    dataset.addValue(dataSource.getCell(row, column), dataSource.hasHeaderRow() ? dataSource
                        .getHeaderRowValue(column) : ("Category " + (column + 1)), dataSource.hasHeaderColumn()
                        ? dataSource.getHeaderColumnValue(row) : ("Series " + (row + 1)));
                }
            }
        } else if ("rows".equals(dataSeries)) {
            for (int row = 0; row < dataSource.getRowCount(); row++) {
                for (int column = 0; column < dataSource.getColumnCount(); column++) {
                    dataset.addValue(dataSource.getCell(row, column), dataSource.hasHeaderColumn() ? dataSource
                        .getHeaderColumnValue(row) : ("Category " + (row + 1)), dataSource.hasHeaderRow() ? dataSource
                        .getHeaderRowValue(column) : ("Series " + (column + 1)));
                }
            }
        } else {
            throw new GenerateException("Invalid series parameter: " + dataSeries);
        }
        return new CategoryPlot(dataset, domainAxis, rangeAxis, renderer);
    }
}
