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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidParamException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class DateChartParam extends DateFormatChartParam
{
    public DateChartParam(String name)
    {
        super(name);
    }

    public DateChartParam(String name, boolean optional)
    {
        super(name, optional);
    }

    @Override
    public Class getType()
    {
        return Date.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value);
        DateFormat format;
        try {
            format = (DateFormat) super.convert(value);
        } catch (ParamException e) {
            format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        }
        try {
            return format.parse(getStringArg(map, "value"));
        } catch (ParseException e) {
            throw new InvalidParamException("Invalid value for parameter :" + getName(), e);
        }
    }
}
