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
package org.xwiki.chart.plot;

import org.xwiki.chart.dataset.DatasetType;
import org.xwiki.chart.axis.AxisType;

/**
 * Enumeration of supported plot types.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public enum PlotType
{
    /** Line plot type. */
    LINE("line", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** Area plot type. */
    AREA("area", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** Bar plot type. */
    BAR("bar", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** Stacked Bar plot type. */
    STACKEDBAR("stackedbar", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** Pie plot type. */
    PIE("pie", DatasetType.PIE),
    /** 3D bar plot type. */
    BAR3D("bar3D", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** Stacked 3D bar plot type. */
    STACKEDBAR3D("stackedbar3D", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** 3D line plot type. */
    LINE3D("line3D", DatasetType.CATEGORY, AxisType.CATEGORY, AxisType.NUMBER),
    /** xy area plot type. */
    XYAREA("xy_area", DatasetType.XY, AxisType.NUMBER, AxisType.NUMBER),
    /** xy line and shape plot type. */
    XYLINEANDSHAPE("xy_line_and_shape", DatasetType.XY, AxisType.NUMBER, AxisType.NUMBER),
    /** xy line3D plot type. */
    XYLINE3D("xy_line3D", DatasetType.XY, AxisType.NUMBER, AxisType.NUMBER),
    /** xy step plot type. */
    XYSTEP("xy_step", DatasetType.XY, AxisType.NUMBER, AxisType.NUMBER);

    /** The name of the plot type. */
    private final String name;

    /** The default dataset for the plot type. */
    private final DatasetType defaultDatasetType;

    /** The default axis types for the plot type. */
    private final AxisType [] defaultAxisTypes;

    /**
     * @param name The name used as value for the plot type parameter.
     * @param defaultDatasetType The default dataset type to use with this plot type.
     * @param defaultAxisTypes The default axis types to use with this plot type.
     */
    PlotType(String name, DatasetType defaultDatasetType, AxisType... defaultAxisTypes)
    {
        this.name = name;
        this.defaultDatasetType = defaultDatasetType;
        this.defaultAxisTypes = defaultAxisTypes;
    }

    /** @return the name (the string representation) of this dataset type. */
    public String getName()
    {
        return name;
    }

    /** @return the default dataset type used by this plot type. */
    public DatasetType getDefaultDatasetType()
    {
        return defaultDatasetType;
    }

    /**
     * @param name A plot type.
     * @return the plot type corresponding to the name, or {@code null}.
     */
    public static PlotType forName(String name)
    {
        for (PlotType plotType : values())
        {
            if (name.equals(plotType.getName())) {
                return plotType;
            }
        }

        return null;
    }

    /**
     * @return the default axis types.
     */
    public AxisType [] getDefaultAxisTypes()
    {
        return defaultAxisTypes;
    }

}
