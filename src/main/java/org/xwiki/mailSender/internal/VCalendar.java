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

package org.xwiki.mailSender.internal;

import java.util.Calendar;
import java.util.Date;

/**
 * For creating vCalendar.
 * 
 * @version $Id$
 */
public class VCalendar
{
    /** Start Date of the event. */
    private Date startDate;

    /** End Date of the event. */ 
    private Date endDate;

    /** Location of the event. */
    private String location;

    /** Summary of the event. */
    private String summary;

    /**
     * Creates a VCalendar.
     * 
     * @param startDate Start Date
     * @param endDate End Date
     * @param location Location
     * @param summary Summary
     */
    public VCalendar(Date startDate, Date endDate, String location, String summary)
    {
        this.startDate = startDate;
        this.endDate = endDate;
        this.summary = summary;
        this.location = location;
    }

    @Override
    public String toString()
    {
        String startDateString = formatDate(startDate);
        String endDateString = formatDate(endDate);
        String creationDate = formatDate(new Date());
        String newLine = "\n";

        String calendar =
            "BEGIN:VCALENDAR" + newLine + "VERSION:2.0" + newLine + "PRODID:-//XWiki//XWiki Calendar 1.0//EN" + newLine
                + "X-WR-TIMEZONE:Europe/Paris" + newLine + "BEGIN:VEVENT" + newLine;
        calendar += "DTSTAMP:" + creationDate + newLine;
        calendar += "LOCATION:" + location + newLine;
        calendar += "DTSTART;TZID=Europe/Paris:" + startDateString + newLine;
        calendar += "DTEND;TZID=Europe/Paris:" + endDateString + newLine;
        calendar += "SUMMARY:" + summary + newLine;
        calendar += "END:VEVENT" + newLine + "END:VCALENDAR";
        return calendar;
    }

    /**
     * Format a date so it can be used in a vCalendar.
     * 
     * @param date Date to be formated
     * @return a string representing the date with the vCalendar format
     */
    private String formatDate(Date date)
    {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        String dateFormatted = "";
        int year = cal.get(Calendar.YEAR);
        String yearAsString = Integer.toString(year);
        /* As january is 0 with Java calendar, add one to the int we get; */
        int month = cal.get(Calendar.MONTH) + 1;
        String monthAsString = Integer.toString(month);
        if (month < 10) {
            monthAsString = "0" + monthAsString;
        }
        int day = cal.get(Calendar.DAY_OF_MONTH);
        String dayAsString = Integer.toString(day);
        if (day < 10) {
            dayAsString = "0" + dayAsString;
        }
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String hourAsString = Integer.toString(hour);
        if (hour < 10) {
            hourAsString = "0" + hourAsString;
        }
        int minutes = cal.get(Calendar.MINUTE);
        String minutesAsString = Integer.toString(minutes);
        if (minutes < 10) {
            minutesAsString = "0" + minutesAsString;
        }
        int seconds = cal.get(Calendar.SECOND);
        String secondsAsString = Integer.toString(seconds);
        if (seconds < 10) {
            secondsAsString = "0" + secondsAsString;
        }
        dateFormatted =
            yearAsString + monthAsString + dayAsString + "T" + hourAsString + minutesAsString + secondsAsString + "Z";
        return dateFormatted;
    }

}
