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
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.axis.DateTickUnit;

import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class DateTickUnitChartParam extends DateFormatChartParam
{
    private Map unitChoice;

    public DateTickUnitChartParam(String name)
    {
        super(name);
        init();
    }

    public DateTickUnitChartParam(String name, boolean optional)
    {
        super(name, optional);
        init();
    }

    @Override
    public void init()
    {
        unitChoice = new HashMap();
        unitChoice.put("day", new Integer(DateTickUnit.DAY));
        unitChoice.put("hour", new Integer(DateTickUnit.HOUR));
        unitChoice.put("millisecond", new Integer(DateTickUnit.MILLISECOND));
        unitChoice.put("minute", new Integer(DateTickUnit.MINUTE));
        unitChoice.put("month", new Integer(DateTickUnit.MONTH));
        unitChoice.put("second", new Integer(DateTickUnit.SECOND));
        unitChoice.put("year", new Integer(DateTickUnit.YEAR));
    }

    @Override
    public Class getType()
    {
        return DateTickUnit.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value);
        DateFormat format;
        int unit = ((Integer) getChoiceArg(map, "unit", unitChoice)).intValue();
        int count = getIntArg(map, "count");
        try {
            return new DateTickUnit(unit, count, (DateFormat) super.convert(value));
        } catch (MissingArgumentException e) {
            return new DateTickUnit(unit, count);
        }
    }
}
