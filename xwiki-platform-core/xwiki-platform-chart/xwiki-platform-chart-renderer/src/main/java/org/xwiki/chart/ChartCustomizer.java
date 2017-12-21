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

import org.jfree.chart.JFreeChart;
import org.xwiki.component.annotation.Role;

/**
 * Allows implementing JFreeChart customizations (colors, fonts, etc) for the drawn graphs.
 *
 * @version $Id$
 * @since 7.4.3
 * @since 8.0RC1
 */
@Role
public interface ChartCustomizer
{
    /**
     * @param jFreeChart the JFree Chart instance representing the graph, on which customizations can be performed
     * @param parameters the parameters passed to the Chart generator and controlling its aspect (e.g. the Chart Macro
     *                   allows user to pass those parameters as a string, for example
     *                   {@code range:B2-D5;series:columns;colors:C3E3F7,1D9FF5,015891,012A45}
     */
    void customize(JFreeChart jFreeChart, Map<String, String> parameters);
}
