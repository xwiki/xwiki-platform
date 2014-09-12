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
package org.xwiki.rendering.internal.macro.chart.source.table;

import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.util.Date;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.general.Dataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Month;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.data.time.Week;
import org.jfree.data.time.Year;
import org.xwiki.chart.time.TimePeriodType;
import org.xwiki.rendering.internal.macro.chart.source.LocaleConfiguration;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * A builder of time table xy datasets ({@see TimeTableXYDataset}) from table data sources.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class TableTimeTableXYDatasetBuilder implements TableDatasetBuilder
{
    /**
     * The name of the time period type parameter.
     */
    public static final String TIMEPERIOD_TYPE_PARAM = "time_period";

    /**
     * Indicates if the transpose of the table should be used.
     */
    private boolean transpose;

    /**
     * The dates.
     */
    private Date[] dates;

    /**
     * The series names.
     */
    private String[] seriesNames;

    /**
     * The dataset.
     */
    private TimeTableXYDataset dataset;

    /**
     * The time period type.
     */
    private TimePeriodType timePeriodType = TimePeriodType.SIMPLE;

    /**
     * Map of time period classes.
     */
    private final Map<TimePeriodType, Class<? extends RegularTimePeriod>> timePeriodClasses
        = new EnumMap<TimePeriodType, Class<? extends RegularTimePeriod>>(TimePeriodType.class) {
            /** Serial version uid. */
            private static final long serialVersionUID = 1L;

            {
                put(TimePeriodType.MILLISECOND, Millisecond.class);
                put(TimePeriodType.SECOND,      Second.class);
                put(TimePeriodType.MINUTE,      Minute.class);
                put(TimePeriodType.HOUR,        Hour.class);
                put(TimePeriodType.DAY,         Day.class);
                put(TimePeriodType.WEEK,        Week.class);
                put(TimePeriodType.MONTH,       Month.class);
                put(TimePeriodType.QUARTER,     Quarter.class);
                put(TimePeriodType.YEAR,        Year.class);
            }
        };

    /**
     * The locale configuration.
     */
    private LocaleConfiguration localeConfiguration;

    @Override
    public void setNumberOfRows(int numberOfRows)
    {
        if (transpose) {
            dates = new Date[numberOfRows];
        } else {
            seriesNames = new String[numberOfRows];
        }
    }

    @Override
    public void setNumberOfColumns(int numberOfColumns)
    {
        if (transpose) {
            seriesNames = new String[numberOfColumns];
        } else {
            dates = new Date[numberOfColumns];
        }
    }

    @Override
    public void setTranspose(boolean transpose)
    {
        this.transpose = transpose;
    }

    /**
     * Set a date in the date series.
     *
     * @param index The index of the date.
     * @param dateText The text form of the date.
     * @throws MacroExecutionException if the date could not be parsed.
     */
    private void setDate(int index, String dateText) throws MacroExecutionException
    {
        try {
            dates[index] = localeConfiguration.getDateFormat().parse(StringUtils.trim(dateText));
        } catch (ParseException e) {
            throw new MacroExecutionException(
                String.format("Failed to parse date [%s] in time table.", dateText), e);
        }
    }

    @Override
    public void setColumnHeading(int columnIndex, String heading) throws MacroExecutionException
    {
        if (transpose) {
            seriesNames[columnIndex] = heading;
        } else {
            setDate(columnIndex, heading);
        }
    }

    @Override
    public void setRowHeading(int rowIndex, String heading) throws MacroExecutionException
    {
        if (transpose) {
            setDate(rowIndex, heading);
        } else {
            seriesNames[rowIndex] = heading;
        }
    }

    /**
     * Get a time period in the series.
     *
     * @param index The index of the time period.
     * @return The corresponding time period.
     */
    private TimePeriod getTimePeriod(int index)
    {

        Date time = dates[index];

        Class<? extends RegularTimePeriod> timePeriodClass = timePeriodClasses.get(timePeriodType);

        if (timePeriodClass == null) {
            Date start = index == 0 ? new Date(0) : dates[index - 1];
            Date end = time;
            return new SimpleTimePeriod(start, end);
        }

        try {
            Constructor<? extends RegularTimePeriod> constructor
                = timePeriodClass.getConstructor(Date.class, TimeZone.class, Locale.class);

            return constructor.newInstance(time, localeConfiguration.getTimeZone(),
                localeConfiguration.getLocale());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setValue(int rowIndex, int columnIndex, Number value) throws MacroExecutionException
    {
        TimePeriod period;

        if (transpose) {
            period = getTimePeriod(rowIndex);
        } else {
            period = getTimePeriod(columnIndex);
        }

        String series;

        if (transpose) {
            series = seriesNames[columnIndex];
        } else {
            series = seriesNames[rowIndex];
        }

        dataset.add(period, value, series, false);
    }

    @Override
    public Dataset getDataset()
    {
        return dataset;
    }

    @Override
    public void setParameters(Map<String, String> parameters) throws MacroExecutionException
    {
        String timePeriodTypeString = parameters.get(TIMEPERIOD_TYPE_PARAM);

        if (timePeriodTypeString != null) {
            this.timePeriodType = TimePeriodType.forName(timePeriodTypeString);
            if (this.timePeriodType == null) {
                throw new MacroExecutionException(String.format("Invalid time period type [%s].",
                    timePeriodTypeString));
            }
        }

        dataset = new TimeTableXYDataset(localeConfiguration.getTimeZone(),
            localeConfiguration.getLocale());
    }

    @Override
    public boolean forceColumnHeadings()
    {
        return transpose;
    }

    @Override
    public boolean forceRowHeadings()
    {
        return !transpose;
    }

    @Override
    public void setLocaleConfiguration(LocaleConfiguration localeConfiguration)
    {
        this.localeConfiguration = localeConfiguration;
    }
}
