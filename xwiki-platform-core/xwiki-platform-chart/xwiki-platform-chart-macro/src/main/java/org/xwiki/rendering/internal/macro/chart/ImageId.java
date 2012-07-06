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
package org.xwiki.rendering.internal.macro.chart;

import org.xwiki.rendering.macro.chart.ChartMacroParameters;

/**
 * Compute a unique id for the image that the chart macro generates.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class ImageId
{
    /**
     * @see ImageId#ImageId(org.xwiki.rendering.macro.chart.ChartMacroParameters)
     */
    private ChartMacroParameters macroParameters;

    /**
     * @param macroParameters the chart macro parameters
     */
    public ImageId(ChartMacroParameters macroParameters)
    {
        this.macroParameters = macroParameters;
    }

    /**
     * Compute a unique id based on the macro parameters.
     *
     * @return the unique image id used for storing the generated chart image
     */
    public String getId()
    {
        return String.format("%s", Math.abs(this.macroParameters.hashCode()));
    }
}
