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

import java.awt.Shape;

import org.jfree.chart.plot.DefaultDrawingSupplier;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class ShapeChartParam extends AbstractChartParam
{
    private ChartParam choice;

    public ShapeChartParam(String name)
    {
        super(name);
        init();
    }

    public ShapeChartParam(String name, boolean optional)
    {
        super(name, optional);
        init();
    }

    public void init()
    {
        choice = new ChoiceChartParam(getName())
        {
            @Override
            protected void init()
            {
                Shape[] shapes = DefaultDrawingSupplier.createStandardSeriesShapes();
                addChoice("square", shapes[0]);
                addChoice("circle", shapes[1]);
                addChoice("triangle-up", shapes[2]);
                addChoice("diamond", shapes[3]);
                addChoice("rectangle-horizontal", shapes[4]);
                addChoice("triangle-down", shapes[5]);
                addChoice("ellipse", shapes[6]);
                addChoice("triangle-right", shapes[7]);
                addChoice("rectangle-vertical", shapes[8]);
                addChoice("triangle-left", shapes[9]);
            }

            @Override
            public Class getType()
            {
                return Shape.class;
            }
        };
    }

    @Override
    public Class getType()
    {
        return Shape.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        try {
            return choice.convert(value);
        } catch (ParamException e) {
            // TODO: custom shapes
            throw e;
        }
    }
}
