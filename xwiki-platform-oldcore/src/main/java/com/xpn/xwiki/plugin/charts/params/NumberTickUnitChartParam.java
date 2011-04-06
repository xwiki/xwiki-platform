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

import java.text.NumberFormat;
import java.util.Map;

import org.jfree.chart.axis.NumberTickUnit;

import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class NumberTickUnitChartParam extends NumberFormatChartParam
{
    public NumberTickUnitChartParam(String name)
    {
        super(name);
    }

    public NumberTickUnitChartParam(String name, boolean optional)
    {
        super(name, optional);
    }

    @Override
    public Class getType()
    {
        return NumberTickUnit.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value);
        double size = getDoubleArg(map, "size");
        try {
            NumberFormat format = (NumberFormat) super.convert(value);
            return new NumberTickUnit(size, format);
        } catch (MissingArgumentException e) {
            return new NumberTickUnit(size);
        }
    }
}
