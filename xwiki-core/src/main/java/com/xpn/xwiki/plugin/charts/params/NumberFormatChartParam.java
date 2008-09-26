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
import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class NumberFormatChartParam extends LocaleChartParam
{
    public static final String TYPE = "type";

    public static final String GENERAL = "general";

    public static final String NUMBER = "number";

    public static final String INTEGER = "integer";

    public static final String CURRENCY = "currency";

    public static final String PERCENT = "percent";

    public NumberFormatChartParam(String name)
    {
        super(name);
    }

    public NumberFormatChartParam(String name, boolean optional)
    {
        super(name, optional);
    }

    @Override
    public Class getType()
    {
        return NumberFormat.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value);
        String type = getStringArg(map, TYPE);
        Locale locale;
        try {
            locale = (Locale) super.convert(value);
        } catch (MissingArgumentException e) {
            locale = null;
        }

        if (type.equals(GENERAL)) {
            if (locale != null) {
                return NumberFormat.getInstance(locale);
            } else {
                return NumberFormat.getInstance();
            }
        } else if (type.equals(NUMBER)) {
            if (locale != null) {
                return NumberFormat.getNumberInstance(locale);
            } else {
                return NumberFormat.getNumberInstance();
            }
        } else if (type.equals(INTEGER)) {
            if (locale != null) {
                return NumberFormat.getIntegerInstance(locale);
            } else {
                return NumberFormat.getIntegerInstance();
            }
        } else if (type.equals(CURRENCY)) {
            if (locale != null) {
                return NumberFormat.getCurrencyInstance(locale);
            } else {
                return NumberFormat.getCurrencyInstance();
            }
        } else if (type.equals(PERCENT)) {
            if (locale != null) {
                return NumberFormat.getPercentInstance(locale);
            } else {
                return NumberFormat.getPercentInstance();
            }
        } else {
            throw new InvalidArgumentException("Invalid value for parameter " + getName()
                + ": Unexpected value for type argument: " + type);
        }
    }
}
