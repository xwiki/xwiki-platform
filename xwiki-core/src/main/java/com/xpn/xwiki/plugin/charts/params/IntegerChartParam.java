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

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class IntegerChartParam extends AbstractChartParam implements ChartParam
{
    public IntegerChartParam(String name)
    {
        super(name);
    }

    public IntegerChartParam(String name, boolean isOptional)
    {
        super(name, isOptional);
    }

    @Override
    public Class getType()
    {
        return Integer.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        try {
            return new Integer(value);
        } catch (NumberFormatException nfe) {
            throw new InvalidParamException("Noninteger value for the " + getName() + " parameter", nfe);
        }
    }
}
