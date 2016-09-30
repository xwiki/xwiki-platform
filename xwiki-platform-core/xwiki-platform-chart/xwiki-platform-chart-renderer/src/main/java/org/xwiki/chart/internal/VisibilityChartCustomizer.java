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

import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.jfree.chart.JFreeChart;
import org.xwiki.chart.ChartCustomizer;
import org.xwiki.component.annotation.Component;

/**
 * Customize visiblity of items on the chart.
 *
 * @version $Id$
 * @since 7.4.3
 * @since 8.0RC1
 */
@Component
@Named("visibility")
@Singleton
public class VisibilityChartCustomizer implements ChartCustomizer
{
    /**
     * Whether the plot border is visible or not.
     */
    private static final String PLOT_BORDER_VISIBLE = "plotBorderVisible";

    /**
     * Whether the legend is visible or not.
     */
    private static final String LEGEND_VISIBLE = "legendVisible";

    @Override
    public void customize(JFreeChart jfchart, Map<String, String> parameters)
    {
        if (parameters.get(PLOT_BORDER_VISIBLE) != null) {
            jfchart.getPlot().setOutlineVisible(Boolean.parseBoolean(parameters.get(PLOT_BORDER_VISIBLE)));
        }

        if (parameters.get(LEGEND_VISIBLE) != null && !Boolean.parseBoolean(parameters.get(LEGEND_VISIBLE))) {
            jfchart.removeLegend();
        }
    }
}
