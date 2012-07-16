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
package com.xpn.xwiki.plugin.charts.plots;

import java.lang.reflect.Constructor;

import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

import com.xpn.xwiki.plugin.charts.ChartCustomizer;
import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class AreaPlotFactory implements PlotFactory
{
    private static AreaPlotFactory uniqueInstance = new AreaPlotFactory();

    private AreaPlotFactory()
    {
        // empty
    }

    public static AreaPlotFactory getInstance()
    {
        return uniqueInstance;
    }

    @Override
    public Plot create(DataSource dataSource, ChartParams params) throws GenerateException, DataSourceException
    {
        Class rendererClass = params.getClass(ChartParams.RENDERER);
        if (rendererClass == null || XYItemRenderer.class.isAssignableFrom(rendererClass)) {
            XYItemRenderer renderer;
            if (rendererClass != null) {
                try {
                    Constructor ctor = rendererClass.getConstructor(new Class[] {});
                    renderer = (XYItemRenderer) ctor.newInstance(new Object[] {});
                } catch (Throwable e) {
                    throw new GenerateException(e);
                }
            } else {
                renderer = new XYAreaRenderer();
            }
            ChartCustomizer.customizeXYItemRenderer(renderer, params);

            return XYPlotFactory.getInstance().create(dataSource, renderer, params);
        } else if (CategoryItemRenderer.class.isAssignableFrom(rendererClass)) {
            CategoryItemRenderer renderer;
            try {
                Constructor ctor = rendererClass.getConstructor(new Class[] {});
                renderer = (CategoryItemRenderer) ctor.newInstance(new Object[] {});
            } catch (Throwable e) {
                throw new GenerateException(e);
            }

            ChartCustomizer.customizeCategoryItemRenderer(renderer, params);

            return CategoryPlotFactory.getInstance().create(dataSource, renderer, params);
        } else {
            throw new GenerateException("Incompatible renderer class: " + rendererClass);
        }
    }
}
