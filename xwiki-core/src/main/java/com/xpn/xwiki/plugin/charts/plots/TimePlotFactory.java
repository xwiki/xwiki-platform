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

import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class TimePlotFactory implements PlotFactory
{
    private static TimePlotFactory uniqueInstance = new TimePlotFactory();

    private TimePlotFactory()
    {
        // empty
    }

    public static TimePlotFactory getInstance()
    {
        return uniqueInstance;
    }

    public Plot create(DataSource dataSource, ChartParams params) throws GenerateException, DataSourceException
    {

        Class rendererClass = params.getClass(ChartParams.RENDERER);
        XYItemRenderer renderer;
        if (rendererClass != null) {
            try {
                Constructor ctor = rendererClass.getConstructor(new Class[] {});
                renderer = (XYItemRenderer) ctor.newInstance(new Object[] {});
            } catch (Throwable e) {
                throw new GenerateException(e);
            }
        } else {
            renderer = new XYLineAndShapeRenderer();
        }
        ChartCustomizer.customizeXYItemRenderer(renderer, params);

        DateAxis domainAxis = new DateAxis();
        ChartCustomizer.customizeDateAxis(domainAxis, params, ChartParams.AXIS_DOMAIN_PREFIX);

        NumberAxis rangeAxis = new NumberAxis();
        rangeAxis.setAutoRangeIncludesZero(false); // override default
        ChartCustomizer.customizeNumberAxis(rangeAxis, params, ChartParams.AXIS_RANGE_PREFIX);

        XYDataset dataset = TimeSeriesCollectionFactory.getInstance().create(dataSource, params);

        XYPlot plot = new XYPlot(dataset, domainAxis, rangeAxis, renderer);

        ChartCustomizer.customizeXYPlot(plot, params);

        return plot;
    }
}
