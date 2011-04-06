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
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.plugin.charts.exceptions.InvalidArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.MissingArgumentException;
import com.xpn.xwiki.plugin.charts.exceptions.ParamException;

public class DateFormatChartParam extends LocaleChartParam
{
    public static final String TYPE = "type";

    public static final String DATE = "date";

    public static final String TIME = "time";

    public static final String DATETIME = "datetime";

    public static final String CUSTOM = "custom";

    public static final String DATE_STYLE = "date_style";

    public static final String TIME_STYLE = "time_style";

    public static final String SHORT = "short";

    public static final String MEDIUM = "medium";

    public static final String LONG = "long";

    public static final String FULL = "full";

    public static final String PATTERN = "pattern";

    private Map styleChoices;

    public DateFormatChartParam(String name)
    {
        super(name);
        init();
    }

    public DateFormatChartParam(String name, boolean optional)
    {
        super(name, optional);
        init();
    }

    public void init()
    {
        styleChoices = new HashMap(4);
        styleChoices.put(SHORT, new Integer(DateFormat.SHORT));
        styleChoices.put(MEDIUM, new Integer(DateFormat.MEDIUM));
        styleChoices.put(LONG, new Integer(DateFormat.LONG));
        styleChoices.put(FULL, new Integer(DateFormat.FULL));
    }

    @Override
    public Class getType()
    {
        return DateFormat.class;
    }

    @Override
    public Object convert(String value) throws ParamException
    {
        Map map = parseMap(value);

        Integer dateStyle, timeStyle;
        try {
            dateStyle = (Integer) getChoiceArg(map, DATE_STYLE, styleChoices);
        } catch (MissingArgumentException e) {
            dateStyle = null;
        }
        try {
            timeStyle = (Integer) getChoiceArg(map, TIME_STYLE, styleChoices);
        } catch (MissingArgumentException e) {
            timeStyle = null;
        }

        Locale locale;
        try {
            locale = (Locale) super.convert(value);
        } catch (MissingArgumentException e) {
            locale = null;
        }

        String type = getStringArg(map, TYPE);

        if (type.equals(DATE)) {
            if (dateStyle != null) {
                if (locale != null) {
                    return DateFormat.getDateInstance(dateStyle.intValue(), locale);
                } else {
                    return DateFormat.getDateInstance(dateStyle.intValue());
                }
            } else {
                return DateFormat.getDateInstance();
            }
        } else if (type.equals(TIME)) {
            if (timeStyle != null) {
                if (locale != null) {
                    return DateFormat.getTimeInstance(timeStyle.intValue(), locale);
                } else {
                    return DateFormat.getTimeInstance(timeStyle.intValue());
                }
            } else {
                return DateFormat.getDateInstance();
            }
        } else if (type.equals(DATETIME)) {
            if (dateStyle != null && timeStyle != null) {
                if (locale != null) {
                    return DateFormat.getDateTimeInstance(dateStyle.intValue(), timeStyle.intValue(), locale);
                } else {
                    return DateFormat.getDateTimeInstance(dateStyle.intValue(), timeStyle.intValue());
                }
            } else {
                return DateFormat.getDateTimeInstance();
            }
        } else if (type.equals(CUSTOM)) {
            String pattern = getStringArg(map, PATTERN);
            if (locale != null) {
                return new SimpleDateFormat(pattern, locale);
            } else {
                return new SimpleDateFormat(pattern);
            }
        } else {
            throw new InvalidArgumentException("Invalid value for parameter " + getName()
                + ": Unexpected value for type argument: " + type);
        }
    }
}
