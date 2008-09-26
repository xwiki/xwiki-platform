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

import java.awt.geom.Point2D;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class Point2DChartParam extends AbstractChartParam
{
    public Point2DChartParam(String name)
    {
        super(name);
    }

    public Point2DChartParam(String name, boolean optional)
    {
        super(name, optional);
    }

    @Override
    public Class getType()
    {
        return Point2D.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value, 2);
        return new Point2D.Double(getDoubleArg(map, "x"), getDoubleArg(map, "y"));
    }
}
