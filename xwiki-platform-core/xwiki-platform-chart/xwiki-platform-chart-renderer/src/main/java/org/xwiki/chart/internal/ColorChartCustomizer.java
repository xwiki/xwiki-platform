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
package org.xwiki.chart.internal;

import java.awt.Color;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.jfree.chart.JFreeChart;
import org.xwiki.chart.ChartCustomizer;
import org.xwiki.component.annotation.Component;

/**
 * Customize chart colors.
 *
 * @version $Id$
 * @since 7.4.3
 * @since 8.0RC1
 */
@Component
@Named("color")
@Singleton
public class ColorChartCustomizer implements ChartCustomizer
{
    /**
     * Background color of the non-chart area.
     */
    private static final String BACKGROUND_COLOR = "backgroundColor";

    /**
     * Background color of the plot area.
     */
    private static final String PLOT_BACKGROUND_COLOR = "plotBackgroundColor";

    /**
     * Color of the plot border.
     */
    private static final String PLOT_BORDER_COLOR = "plotBorderColor";

    /**
     * Color of the outer graph border.
     */
    private static final String BORDER_COLOR = "borderColor";

    /**
     * Background color of the legend.
     */
    private static final String LEGEND_BACKGROUND_COLOR = "legendBackgroundColor";

    @Override
    public void customize(JFreeChart jfchart, Map<String, String> parameters)
    {
        // Set the default colors to use if the user has specified some colors.
        DrawingSupplierFactory drawingSupplierFactory = new DrawingSupplierFactory();
        jfchart.getPlot().setDrawingSupplier(drawingSupplierFactory.createDrawingSupplier(parameters));

        // Set any plot background color if the user has specified one
        if (parameters.get(PLOT_BACKGROUND_COLOR) != null) {
            jfchart.getPlot().setBackgroundPaint(convertColor(parameters.get(PLOT_BACKGROUND_COLOR)));
        }

        // Set the non-plot area background color if specified
        if (parameters.get(BACKGROUND_COLOR) != null) {
            jfchart.setBackgroundPaint(convertColor(parameters.get(BACKGROUND_COLOR)));
        }

        // Set the legend background color if specified
        if (parameters.get(LEGEND_BACKGROUND_COLOR) != null) {
            jfchart.getLegend().setBackgroundPaint(convertColor(parameters.get(LEGEND_BACKGROUND_COLOR)));
        }

        // Set the plot border color if specified
        if (parameters.get(PLOT_BORDER_COLOR) != null) {
            jfchart.getPlot().setOutlinePaint(convertColor(parameters.get(PLOT_BORDER_COLOR)));
        }

        // Set the graph border color if specified
        if (parameters.get(BORDER_COLOR) != null) {
            jfchart.setBorderPaint(convertColor(parameters.get(BORDER_COLOR)));
        }
    }

    private Color convertColor(String colorInHex)
    {
        int red = Integer.parseInt(colorInHex.substring(0, 2), 16);
        int green = Integer.parseInt(colorInHex.substring(2, 4), 16);
        int blue = Integer.parseInt(colorInHex.substring(4, 6), 16);
        return new Color(red, green, blue);
    }
}
