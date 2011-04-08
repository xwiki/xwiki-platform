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
 *
 */
package com.xpn.xwiki.plugin.charts.params;

import org.jfree.chart.axis.CategoryLabelPositions;

// TODO: maybe extend this class sometime
public class CategoryLabelPositionsChartParam extends ChoiceChartParam
{

    public CategoryLabelPositionsChartParam(String name)
    {
        super(name);
    }

    public CategoryLabelPositionsChartParam(String name, boolean optional)
    {
        super(name, optional);
    }

    @Override
    protected void init()
    {
        addChoice("down_45", CategoryLabelPositions.DOWN_45);
        addChoice("down_90", CategoryLabelPositions.DOWN_90);
        addChoice("standard", CategoryLabelPositions.STANDARD);
        addChoice("up_45", CategoryLabelPositions.UP_45);
        addChoice("up_90", CategoryLabelPositions.UP_90);
    }

    @Override
    public Class getType()
    {
        return CategoryLabelPositions.class;
    }
}
