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

import java.lang.reflect.Constructor;

import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class PiePlotFactory implements PlotFactory
{
    private static PiePlotFactory uniqueInstance = new PiePlotFactory();

    private PiePlotFactory()
    {
        // empty
    }

    public static PiePlotFactory getInstance()
    {
        return uniqueInstance;
    }

    public Plot create(DataSource dataSource, ChartParams params) throws GenerateException, DataSourceException
    {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String dataSeries = params.getString(ChartParams.SERIES);
        if (dataSeries.equals("columns")) {
            for (int row = 0; row < dataSource.getRowCount(); row++) {
                if (dataSource.hasHeaderColumn()) {
                    String category = dataSource.getHeaderColumnValue(row);
                    dataset.setValue(category, dataSource.getCell(row, 0));
                } else {
                    dataset.setValue("Category " + (row + 1), dataSource.getCell(row, 0));
                }
            }
        } else if (dataSeries.equals("rows")) {
            for (int column = 0; column < dataSource.getColumnCount(); column++) {
                if (dataSource.hasHeaderRow()) {
                    String category = dataSource.getHeaderRowValue(column);
                    dataset.setValue(category, dataSource.getCell(0, column));
                } else {
                    dataset.setValue("Category " + (column + 1), dataSource.getCell(0, column));
                }
            }
        } else {
            throw new GenerateException("Invalid series parameter:" + dataSeries);
        }

        Class plotClass = params.getClass(ChartParams.RENDERER);
        PiePlot plot;
        if (plotClass != null) {
            try {
                Constructor ctor = plotClass.getConstructor(new Class[] {PieDataset.class});
                plot = (PiePlot) ctor.newInstance(new Object[] {dataset});
            } catch (Throwable e) {
                throw new GenerateException(e);
            }
        } else {
            plot = new PiePlot(dataset);
        }
        ChartCustomizer.customizePiePlot(plot, params);
        return plot;
    }
}
