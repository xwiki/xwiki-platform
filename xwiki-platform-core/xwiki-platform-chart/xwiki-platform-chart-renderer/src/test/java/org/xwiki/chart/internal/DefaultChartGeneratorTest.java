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

import org.jfree.chart.plot.Plot;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.chart.ChartGenerator;
import org.xwiki.chart.ChartGeneratorException;
import org.xwiki.chart.internal.plot.PlotGenerator;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultChartGenerator}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultChartGeneratorTest
{
    @InjectMockComponents
    private DefaultChartGenerator chartGenerator;

    @MockComponent
    private ChartConfiguration configuration;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @Mock
    private PlotGenerator plotGenerator;

    @Test
    void oversizedChart() throws ComponentLookupException, ChartGeneratorException
    {
        ChartModel model = mock(ChartModel.class);
        String type = "fooType";
        String title = "some title";

        when(componentManager.getInstance(PlotGenerator.class, type)).thenReturn(this.plotGenerator);
        Plot plot = mock(Plot.class);
        when(plotGenerator.generate(eq(model), anyMap())).thenReturn(plot);

        when(configuration.getMaximumChartHeight()).thenReturn(10);
        when(configuration.getMaximumChartWidth()).thenReturn(20);

        Map<String, String> parameters = Map.of(
            ChartGenerator.TYPE_PARAM, type,
            ChartGenerator.HEIGHT_PARAM, "8",
            ChartGenerator.WIDTH_PARAM, "18"
        );
        assertNotNull(chartGenerator.generate(model, parameters));

        parameters = Map.of(
            ChartGenerator.TYPE_PARAM, type,
            ChartGenerator.HEIGHT_PARAM, "10",
            ChartGenerator.WIDTH_PARAM, "20"
        );
        assertNotNull(chartGenerator.generate(model, parameters));

        Map<String, String> parametersErr1 = Map.of(
            ChartGenerator.TYPE_PARAM, type,
            ChartGenerator.HEIGHT_PARAM, "11",
            ChartGenerator.WIDTH_PARAM, "20"
        );
        ChartGeneratorException chartGeneratorException =
            assertThrows(ChartGeneratorException.class, () -> chartGenerator.generate(model, parametersErr1));
        assertEquals("Maximum chart width or height exceeded (limit is: [20]x[10]).",
            chartGeneratorException.getMessage());

        Map<String, String> parametersErr2 = Map.of(
            ChartGenerator.TYPE_PARAM, type,
            ChartGenerator.HEIGHT_PARAM, "2",
            ChartGenerator.WIDTH_PARAM, "21"
        );
        chartGeneratorException =
            assertThrows(ChartGeneratorException.class, () -> chartGenerator.generate(model, parametersErr2));
        assertEquals("Maximum chart width or height exceeded (limit is: [20]x[10]).", chartGeneratorException.getMessage());

        Map<String, String> parametersErr3 = Map.of(
            ChartGenerator.TYPE_PARAM, type,
            ChartGenerator.HEIGHT_PARAM, "11",
            ChartGenerator.WIDTH_PARAM, "21"
        );
        chartGeneratorException =
            assertThrows(ChartGeneratorException.class, () -> chartGenerator.generate(model, parametersErr3));
        assertEquals("Maximum chart width or height exceeded (limit is: [20]x[10]).", chartGeneratorException.getMessage());

        verify(plotGenerator, times(2)).generate(eq(model), anyMap());
    }
}