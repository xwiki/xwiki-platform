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

import java.util.Map;

import org.jfree.ui.RectangleInsets;

import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class RectangleInsetsChartParam extends AbstractChartParam
{
    public RectangleInsetsChartParam(String name)
    {
        super(name);
    }

    public RectangleInsetsChartParam(String name, boolean optional)
    {
        super(name, optional);
    }

    @Override
    public Class getType()
    {
        return RectangleInsets.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value, 4);
        return new RectangleInsets(getDoubleArg(map, "top"), getDoubleArg(map, "left"), getDoubleArg(map, "bottom"),
            getDoubleArg(map, "right"));
    }
}
