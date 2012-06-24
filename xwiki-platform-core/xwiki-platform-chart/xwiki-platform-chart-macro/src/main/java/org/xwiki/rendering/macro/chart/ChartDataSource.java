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
package org.xwiki.rendering.macro.chart;

import java.util.Map;

import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * Interface for defining various data sources for charts.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Role
public interface ChartDataSource
{
    /**
     * Parameter identifier for data source.
     */
    String SOURCE = "source";
    
    /**
     * Parameter identifier for data source specific parameters.
     */
    String PARAMS = "params";    
    
    /**
     * Decodes the given macroContent / extraParams and builds a {@link ChartModel}.
     * 
     * @param macroContent content of the macro.
     * @param macroParameters parameters provided for the macro.
     * @return a {@link ChartModel} corresponding to the parameters passed in.
     * @throws MacroExecutionException if something goes wrong while decoding source / parameters.
     */
    ChartModel buildModel(String macroContent, Map<String, String> macroParameters) throws MacroExecutionException;
}
