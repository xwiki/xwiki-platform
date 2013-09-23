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

import org.jfree.chart.plot.Plot;
import org.xwiki.chart.PlotGeneratorException;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.annotation.Role;

/**
 * Interface for defining various jfreechart {@link Plot} generators.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Role
public interface PlotGenerator
{
    /**
     * Generates a {@link Plot} for the given {@link ChartModel} and parameters. 
     * 
     * @param model the {@link ChartModel} instance.
     * @param parameters extra parameters.
     * @return the generated {@link Plot}.
     * @throws PlotGeneratorException if the dataset or the axes does not match the plot.
     */
    Plot generate(ChartModel model, Map<String, String> parameters) throws PlotGeneratorException;
}
