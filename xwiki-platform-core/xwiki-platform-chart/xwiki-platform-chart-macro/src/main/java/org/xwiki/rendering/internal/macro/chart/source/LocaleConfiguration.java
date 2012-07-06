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
 */
package org.xwiki.rendering.internal.macro.chart.source;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang3.LocaleUtils;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * A configuration object for locale and time zone.
 *
 * This super class provides basic parameter validation.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class LocaleConfiguration extends AbstractConfigurator
{
    /**
     * The name of the locale parameter.
     */
    public static final String LOCALE_PARAM = "locale";

    /**
     * The name of the dateformat parameter.
     */
    public static final String DATEFORMAT_PARAM = "date_format";

    /**
     * The locale used for generating date format.
     */
    private Locale locale = Locale.getDefault();

    /**
     * The time zone.
     */
    private TimeZone timeZone = TimeZone.getDefault();

    /**
     * The dateformat.  Default is taken from the server locale.
     */
    private DateFormat dateFormat = DateFormat.getInstance();

    /**
     * A configured date format pattern string.
     */
    private String dateFormatString;

    /**
     * Let an implementation set a parameter.
     *
     * @param key The key of the parameter.
     * @param value The value of the parameter.
     * @return {@code true} if the parameter was claimed.
     * @throws MacroExecutionException if the parameter is invalid in some way.
     */
    public boolean setParameter(String key, String value) throws MacroExecutionException
    {
        boolean claimed = true;

        if (LOCALE_PARAM.equals(key)) {
            boolean valid = true;
            Locale l;
            try {
                l = LocaleUtils.toLocale(value);
                if (!LocaleUtils.isAvailableLocale(l)) {
                    valid = false;
                } else {
                    this.locale = l;
                }
            } catch (IllegalArgumentException e) {
                valid = false;
            }
            if (!valid) {
                throw new MacroExecutionException(String.format("Invalid locale string [%s].", value));
            }
        } else if (DATEFORMAT_PARAM.equals(key)) {
            this.dateFormatString = value;
        } else {
            claimed = false;
        }

        return claimed;
    }

    @Override
    public void validateParameters() throws MacroExecutionException
    {
        try {
            if (dateFormatString != null) {
                dateFormat = new SimpleDateFormat(dateFormatString, locale);
            }
        } catch (IllegalArgumentException e) {
            throw new MacroExecutionException(String.format("Invalid date format [%s].", dateFormatString));
        }
    }

    /**
     * @return the date format.
     */
    public DateFormat getDateFormat()
    {
        return dateFormat;
    }

    /**
     * @return the locale.
     */
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * @return the time zone.
     */
    public TimeZone getTimeZone()
    {
        return timeZone;
    }
}
