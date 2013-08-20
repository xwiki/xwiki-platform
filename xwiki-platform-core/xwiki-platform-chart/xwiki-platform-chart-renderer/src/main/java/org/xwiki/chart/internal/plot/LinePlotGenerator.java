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

import javax.inject.Named;
import javax.inject.Singleton;

import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.xwiki.component.annotation.Component;

/**
 * A {@link PlotGenerator} for generating line charts.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("line")
@Singleton
public class LinePlotGenerator extends AbstractCategoryPlotGenerator
{
    @Override
    protected CategoryItemRenderer getRenderer(Map<String, String> parameters)
    {
        boolean linesVisible = !Boolean.valueOf(parameters.get("hideLines"));
        boolean shapesVisible = !Boolean.valueOf(parameters.get("hideShapes"));
        return new LineAndShapeRenderer(linesVisible, shapesVisible);
    }
}
