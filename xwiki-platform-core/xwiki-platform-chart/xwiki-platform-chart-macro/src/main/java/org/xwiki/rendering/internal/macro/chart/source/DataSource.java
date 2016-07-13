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
package org.xwiki.rendering.internal.macro.chart.source;

import java.util.Map;

import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.annotation.Role;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * A data source is able to provide a data set for chart generation.
 *
 * @version $Id$
 * @since 4.2M1
 */
@Role
public interface DataSource
{
    /**
     * Parameter identifier for data source.
     */
    String SOURCE_PARAM = "source";
    /**
     * Parameter identifier for data source specific parameters.
     */
    String PARAMS_PARAM = "params";

    /**
     * Decodes the given macroContent / extraParams and builds a Data Set.
     *
     * @param macroContent the content of the macro
     * @param parameters the parameters provided for the source
     * @param context the macro transformation context, used for example to find out the current document reference
     * @throws MacroExecutionException if something goes wrong while decoding source / parameters
     */
    void buildDataset(String macroContent, Map<String, String> parameters, MacroTransformationContext context)
        throws MacroExecutionException;

    /**
     * {@link #buildDataset} must be called before this method.
     *
     * @return the {@link ChartModel} for the dataset.
     */
    ChartModel getChartModel();
}
