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

import java.util.ArrayList;
import java.util.List;

import org.jfree.chart.axis.Axis;
import org.jfree.data.general.Dataset;
import org.xwiki.chart.model.ChartModel;

/**
 * A chart model implementation that simply holds a dataset and axis configurations.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class SimpleChartModel implements ChartModel
{
    /**
     * The dataset.
     */
    private Dataset dataset;

    /**
     * The axes.
     */
    private final List<Axis> axes = new ArrayList<Axis>();

    /**
     * Public constructor.
     */
    public SimpleChartModel()
    {
    }

    /**
     * @param dataset The dataset.
     */
    void setDataset(Dataset dataset)
    {
        this.dataset = dataset;
    }

    @Override
    public Dataset getDataset()
    {
        return dataset;
    }

    @Override
    public Axis getAxis(int index)
    {
        return axes.get(index);
    }

    /**
     * Set the axis at the given index.
     *
     * @param index The index.
     * @param axis The axis.
     */
    void setAxis(int index, Axis axis)
    {
        while (index > axes.size()) {
            axes.add(null);
        }

        if (index == axes.size()) {
            axes.add(axis);
        } else {
            axes.set(index, axis);
        }
    }

    /**
     * @param axis The axis to add.
     */
    void addAxis(Axis axis)
    {
        axes.add(axis);
    }
}
