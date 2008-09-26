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
package com.xpn.xwiki.plugin.charts.params;

import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.renderer.category.AreaRenderer;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.chart.renderer.category.CategoryStepRenderer;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.chart.renderer.category.IntervalBarRenderer;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.chart.renderer.category.LevelRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.category.LineRenderer3D;
import org.jfree.chart.renderer.category.StackedAreaRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StackedBarRenderer3D;
import org.jfree.chart.renderer.category.WaterfallBarRenderer;
import org.jfree.chart.renderer.xy.ClusteredXYBarRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer;
import org.jfree.chart.renderer.xy.StackedXYAreaRenderer2;
import org.jfree.chart.renderer.xy.StackedXYBarRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer;
import org.jfree.chart.renderer.xy.XYAreaRenderer2;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLine3DRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYStepAreaRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;

public class RendererClassChartParam extends ChoiceChartParam
{
    public RendererClassChartParam(String name)
    {
        super(name);
    }

    public RendererClassChartParam(String name, boolean isOptional)
    {
        super(name, isOptional);
    }

    @Override
    protected void init()
    {
        // Pie (an anomaly)
        addChoice("pie", PiePlot.class);
        addChoice("pie_3d", PiePlot3D.class);
        addChoice("ring", RingPlot.class);

        // Bar (CategoryPlot)
        addChoice("bar", BarRenderer.class);
        addChoice("bar_3d", BarRenderer3D.class);
        addChoice("waterfall_bar", WaterfallBarRenderer.class);
        addChoice("interval_bar", IntervalBarRenderer.class);
        addChoice("layered_bar", LayeredBarRenderer.class);
        addChoice("stacked_bar", StackedBarRenderer.class);
        addChoice("stacked_bar_3d", StackedBarRenderer3D.class);
        addChoice("level", LevelRenderer.class);
        addChoice("grouped_stacked_bar", GroupedStackedBarRenderer.class); // pretty much useless, unless groups can be
                                                                           // defined
        // addChoice("gantt", GanttRenderer.class); // looks like a default bar chart (which is not good)

        // Bar (XYPlot)
        addChoice("xy_bar", XYBarRenderer.class);
        addChoice("xy_clustered_bar", ClusteredXYBarRenderer.class);
        addChoice("xy_stacked_bar", StackedXYBarRenderer.class);

        // Line (CategoryPlot)
        addChoice("line_and_shape", LineAndShapeRenderer.class);
        addChoice("line_3d", LineRenderer3D.class);
        addChoice("step", CategoryStepRenderer.class);

        // Line and Time (XYPlot)
        addChoice("xy_line_and_shape", XYLineAndShapeRenderer.class);
        addChoice("xy_line_3d", XYLine3DRenderer.class);
        addChoice("xy_step", XYStepRenderer.class);

        // Area (CategoryPlot)
        addChoice("area", AreaRenderer.class);
        addChoice("stacked_area", StackedAreaRenderer.class);

        // Area (XYPlot)
        addChoice("xy_area", XYAreaRenderer.class);
        addChoice("xy_area2", XYAreaRenderer2.class);
        addChoice("xy_stacked_area", StackedXYAreaRenderer.class);
        addChoice("xy_stacked_area2", StackedXYAreaRenderer2.class);
        addChoice("xy_step_area", XYStepAreaRenderer.class);
    }

    @Override
    public Class getType()
    {
        return Class.class;
    }
}
