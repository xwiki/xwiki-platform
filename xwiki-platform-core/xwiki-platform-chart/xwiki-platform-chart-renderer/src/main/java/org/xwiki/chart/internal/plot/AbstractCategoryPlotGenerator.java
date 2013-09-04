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

import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;
import org.xwiki.chart.PlotGeneratorException;
import org.xwiki.chart.model.ChartModel;

/**
 * Generate Plots for Category data sets.
 * 
 * @version $Id$
 * @since 4.1M1
 */
public abstract class AbstractCategoryPlotGenerator implements PlotGenerator
{
    @Override
    public Plot generate(ChartModel model, Map<String, String> parameters) throws PlotGeneratorException
    {
        CategoryDataset dataset;
        CategoryAxis domainAxis;
        ValueAxis rangeAxis;

        if (model.getDataset() instanceof CategoryDataset) {
            dataset = (CategoryDataset) model.getDataset();
        } else {
            throw new PlotGeneratorException("Incompatible dataset for category plot.");
        }

        if (model.getAxis(0) instanceof CategoryAxis) {
            domainAxis = (CategoryAxis) model.getAxis(0);
        } else {
            throw new PlotGeneratorException("Incompatible axis 0 for category plot.");
        }

        if (model.getAxis(1) instanceof ValueAxis) {
            rangeAxis = (ValueAxis) model.getAxis(1);
        } else {
            throw new PlotGeneratorException("Incompatible axis 1 for category plot.");
        }

        return new CategoryPlot(dataset, domainAxis, rangeAxis, getRenderer(parameters));
    }

    /**
     * @param parameters used to configure the renderer
     * @return an {@link CategoryItemRenderer} to be used for plotting the chart.
     */
    protected abstract CategoryItemRenderer getRenderer(Map<String, String> parameters);

}
