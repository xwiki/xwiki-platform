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

import java.util.Map;

import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.annotation.Role;

/**
 * A component interface for defining various chart generators.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Role
public interface ChartGenerator
{
    /**
     * Title parameter identifier.
     */
    String TITLE_PARAM = "title";

    /**
     * Height parameter identifier.
     */
    String HEIGHT_PARAM = "height";

    /**
     * Width parameter identifier.
     */
    String WIDTH_PARAM = "width";

    /**
     * Type parameter identifier.
     */
    String TYPE_PARAM = "type";    
    
    /**
     * Source parameter identifier.
     */
    String SERIES_PARAM = "series";        

    /**
     * Color parameter identifier. The format is {@code color1,color2,...,colorN} where each color is specified as
     * a 6 characters string, the first 2 representing in hexadecimal the red percentage, the next two the green
     * percentage and the last 2 the blue percentage. For example {@code FF0000,00FF00,0000FF} for red, green, blue.
     */
    String COLORS_PARAM = "colors";

    /**
     * Generates an image of a chart representing the data presented as a {@link ChartModel} and extra formatting
     * parameters provided in the parameters map.
     * 
     * @param model the {@link ChartModel} which defines the data model to be represented by the chart.
     * @param parameters extra parameters for controlling various features of the image output.
     * @return the chart image (binary).
     * @throws ChartGeneratorException if the {@link ChartGenerator} is unable to render a chart.
     */
    byte[] generate(ChartModel model, Map<String, String> parameters) throws ChartGeneratorException;
}
