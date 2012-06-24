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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Plot;
import org.xwiki.chart.ChartGenerator;
import org.xwiki.chart.ChartGeneratorException;
import org.xwiki.chart.internal.plot.AreaPlotGenerator;
import org.xwiki.chart.internal.plot.Bar3DPlotGenerator;
import org.xwiki.chart.internal.plot.BarPlotGenerator;
import org.xwiki.chart.internal.plot.Line3DPlotGenerator;
import org.xwiki.chart.internal.plot.LinePlotGenerator;
import org.xwiki.chart.internal.plot.PiePlotGenerator;
import org.xwiki.chart.internal.plot.PlotGenerator;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;

/**
 * The default implementation of {@link ChartGenerator} component interface which utilizes the jfreechart charting
 * library.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
public class DefaultChartGenerator implements ChartGenerator, Initializable
{
    /**
     * Map of available plot generators.
     */
    private Map<String, PlotGenerator> plotGenerators;

    /**
     * Allows providing custom colors.
     */
    private DrawingSupplierFactory drawingSupplierFactory = new DrawingSupplierFactory();

    @Override
    public void initialize() throws InitializationException
    {
        plotGenerators = new HashMap<String, PlotGenerator>();        
        plotGenerators.put("line", new LinePlotGenerator());
        plotGenerators.put("bar", new BarPlotGenerator());    
        plotGenerators.put("area", new AreaPlotGenerator());
        plotGenerators.put("pie", new PiePlotGenerator());
        plotGenerators.put("line3D", new Line3DPlotGenerator());
        plotGenerators.put("bar3D", new Bar3DPlotGenerator());
    }

    @Override
    public byte[] generate(ChartModel model, Map<String, String> parameters) throws ChartGeneratorException
    {
        setDefaultParams(parameters);
        String type = parameters.get(TYPE_PARAM);
        String title = parameters.get(TITLE_PARAM);       
        PlotGenerator generator = plotGenerators.get(type);
        if (null == generator) {
            throw new ChartGeneratorException(String.format("No such chart type : [%s].", type));
        }
        Plot plot = generator.generate(model, parameters);

        // Set the default colors to use if the user has specified some colors.
        plot.setDrawingSupplier(this.drawingSupplierFactory.createDrawingSupplier(parameters));

        JFreeChart jfchart = new JFreeChart(title, plot);
        int width = Integer.parseInt(parameters.get(WIDTH_PARAM));
        int height = Integer.parseInt(parameters.get(HEIGHT_PARAM));
        try {
            return ChartUtilities.encodeAsPNG(jfchart.createBufferedImage(width, height));
        } catch (IOException ex) {
            throw new ChartGeneratorException("Error while png encoding the chart image.");
        }
    }
    
    /**
     * Sets up default values for certain parameters, if they are not provided.
     * 
     * @param parameters the parameter set which the values are to be put into
     */
    public void setDefaultParams(Map<String, String> parameters)
    {
        setParam(SERIES_PARAM, "rows", parameters);
        setParam(WIDTH_PARAM, "400", parameters);
        setParam(HEIGHT_PARAM, "300", parameters);
    }

    /**
     * Sets up values in a map, unless they already exist.
     * 
     * @param key the key
     * @param value the corresponding value
     * @param map the map in which the values are put
     */
    private static void setParam(String key, String value, Map<String, String> map)
    {
        if (!map.containsKey(key)) {
            map.put(key, value);
        }
    }
}
