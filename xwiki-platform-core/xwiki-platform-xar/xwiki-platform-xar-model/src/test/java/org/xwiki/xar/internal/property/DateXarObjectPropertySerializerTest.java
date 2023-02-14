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
package org.xwiki.xar.internal.property;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link DateXarObjectPropertySerializer}.
 * 
 * @version $Id$
 */
class DateXarObjectPropertySerializerTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void parseDate() throws ParseException
    {
        Date newDate = new Date();
        DateFormat format = new SimpleDateFormat(DateXarObjectPropertySerializer.DEFAULT_FORMAT);
        String string = format.format(newDate);
        Date formattedDate = format.parse(string);

        assertEquals(formattedDate, DateXarObjectPropertySerializer.parseDate(string));
    }

    @Test
    void parseDateOld() throws ParseException
    {
        Date newDate = new Date();
        DateFormat format = new SimpleDateFormat("EEE MMM d HH:mm:ss z yyyy", Locale.US);
        String string = format.format(newDate);
        Date formattedDate = format.parse(string);

        assertEquals(formattedDate, DateXarObjectPropertySerializer.parseDate(string));

        assertEquals(
            "Failed to parse date [" + string + "] using format [yyyy-MM-dd HH:mm:ss.S]."
                + " Trying again with format [EEE MMM d HH:mm:ss z yyyy].",
            this.logCapture.getLogEvent(0).getFormattedMessage());
    }

    @Test
    void parseDateInvalid()
    {
        Date newDate = new Date();
        assertTrue(DateXarObjectPropertySerializer.parseDate("not date").after(newDate));

        assertEquals(
            "Failed to parse date [not date] using format [yyyy-MM-dd HH:mm:ss.S]."
                + " Trying again with format [EEE MMM d HH:mm:ss z yyyy].",
            this.logCapture.getLogEvent(0).getFormattedMessage());
        assertEquals("Failed to parse date [not date] using format [EEE MMM d HH:mm:ss z yyyy]."
            + " Defaulting to the current date.", this.logCapture.getLogEvent(1).getFormattedMessage());
    }

    @Test
    void serializeDate()
    {
        Date date = new Date();
        DateFormat format = new SimpleDateFormat(DateXarObjectPropertySerializer.DEFAULT_FORMAT);
        String string = format.format(date);

        assertEquals(string, DateXarObjectPropertySerializer.serializeDate(date));
    }

    @Test
    void serializeDateNull()
    {
        assertEquals("", DateXarObjectPropertySerializer.serializeDate(null));
    }
}
