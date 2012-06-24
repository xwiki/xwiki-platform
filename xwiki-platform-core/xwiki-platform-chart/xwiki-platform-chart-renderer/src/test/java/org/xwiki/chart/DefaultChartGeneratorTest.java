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
package org.xwiki.chart;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.xwiki.chart.internal.DefaultChartGenerator;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.chart.model.DefaultChartModel;
import org.xwiki.test.AbstractComponentTestCase;

/**
 * Test case for {@link DefaultChartGenerator}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultChartGeneratorTest extends AbstractComponentTestCase
{
    /**
     * The {@link ChartGenerator} component.
     */
    private ChartGenerator chartGenerator;

    /**
     * The {@link ChartModel}.
     */
    private ChartModel model;

    @Override
    protected void registerComponents() throws Exception
    {
        Short dataArray[][] = { {1, 2, 3}, {1, 3, 5}};
        String rowHeaders[] = {"1", "2"};
        String columnHeaders[] = {"1", "2", "3"};
        this.model = new DefaultChartModel(dataArray, rowHeaders, columnHeaders);
        this.chartGenerator = getComponentManager().getInstance(ChartGenerator.class);
    }

    @Test
    public final void testPieChart() throws ChartGeneratorException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test pie chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "pie");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }

    @Test
    public final void testBarChart() throws ChartGeneratorException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test bar chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "bar");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }

    @Test
    public final void testBar3DChart() throws ChartGeneratorException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test 3D bar chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "bar3D");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }

    @Test
    public final void testLineChart() throws ChartGeneratorException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test line chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "line");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }

    @Test
    public final void testLine3DChart() throws ChartGeneratorException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test 3D line chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "line3D");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }

    @Test
    public final void testAreaChart() throws ChartGeneratorException
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test area chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "area");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }

    @Test
    public final void testCustomColors() throws Exception
    {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(ChartGenerator.TITLE_PARAM, "Test area chart");
        parameters.put(ChartGenerator.TYPE_PARAM, "area");
        parameters.put(ChartGenerator.SERIES_PARAM, "rows");
        parameters.put(ChartGenerator.COLORS_PARAM, "FF0000,00FF00,0000FF");

        byte[] chart = chartGenerator.generate(model, parameters);
        Assert.assertNotNull(chart);
    }
}
