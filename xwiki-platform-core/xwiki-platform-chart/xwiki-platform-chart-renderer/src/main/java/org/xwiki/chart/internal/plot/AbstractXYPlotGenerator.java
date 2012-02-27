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

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.xwiki.chart.model.ChartModel;

/**
 * An abstract {@link PlotGenerator} for defining various XY type charts.
 *
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractXYPlotGenerator implements PlotGenerator
{
    @Override
    public Plot generate(ChartModel model, Map<String, String> parameters)
    {
        NumberAxis domainAxis = new NumberAxis();
        NumberAxis rangeAxis = new NumberAxis();
        XYDataset dataset = buildXYDataset(model, parameters);
        return new XYPlot(dataset, domainAxis, rangeAxis, getXYItemRenderer(parameters));
    }

    /**
     * Builds an {@link XYDataset} corresponding to the provided {@link ChartModel}.
     * 
     * @param model the {@link ChartModel} instance.
     * @param params additional parameters.
     * @return an {@link XYDataset} corresponding to the provided {@link ChartModel}.
     */
    protected XYDataset buildXYDataset(ChartModel model, Map<String, String> params)
    {
        String dataSeries = params.get("series");
        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        if (dataSeries.equals("rows")) {
            extractRows(dataset, model);
        } else {
            extractColumns(dataset, model);
        }
        return dataset;
    }
    
    /**
     * Extracts data rows from the {@link ChartModel} provided and populates the {@link DefaultTableXYDataset}
     * accordingly.
     * 
     * @param model the {@link ChartModel} instance.
     * @param dataset the {@link DefaultTableXYDataset} to be populated.
     */
    private void extractRows(DefaultTableXYDataset dataset, ChartModel model)
    {
        for (int row = 0; row < model.getRowCount(); row++) {
            XYSeries series = new XYSeries(model.getRowHeader(row), false, false);
            for (int column = 0; column < model.getColumnCount(); column++) {
                series.add(column, model.getCellValue(row, column));
            }
            dataset.addSeries(series);
        }
    }

    /**
     * Extracts data columns from the {@link ChartModel} provided and populates the {@link DefaultTableXYDataset}
     * accordingly.
     * 
     * @param model the {@link ChartModel} instance.
     * @param dataset the {@link DefaultTableXYDataset} to be populated.
     */
    private void extractColumns(DefaultTableXYDataset dataset, ChartModel model)
    {
        for (int column = 0; column < model.getColumnCount(); column++) {
            XYSeries series = new XYSeries(model.getColumnHeader(column), false, false);
            for (int row = 0; row < model.getRowCount(); row++) {
                series.add(row, model.getCellValue(row, column));
            }
            dataset.addSeries(series);
        }
    }    
    
    /**
     * Returns the {@link XYItemRenderer} to be used for plotting the chart.
     * 
     * @param parameters additional parameters.
     * @return an {@link XYItemRenderer} to be used for plotting the chart.
     */
    protected abstract XYItemRenderer getXYItemRenderer(Map<String, String> parameters);
}
