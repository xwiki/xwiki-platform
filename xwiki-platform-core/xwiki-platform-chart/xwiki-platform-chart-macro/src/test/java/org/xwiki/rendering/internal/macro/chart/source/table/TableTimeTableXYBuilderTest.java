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

import org.junit.Assert;
import org.junit.Test;
import org.jfree.data.time.TimeTableXYDataset;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.axis.DateAxis;

import org.xwiki.chart.model.ChartModel;

import java.util.Date;
import java.util.Locale;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Tests building a time table xy dataset from a table data source.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public class TableTimeTableXYBuilderTest extends AbstractMacroContentTableBlockDataSourceTest
{
    @Test
    public void testBuildTimeTableXY() throws Exception
    {
        String content =
            "| Date | column 2 | column 3 | column 4\n" +
            "| 2012-01-01 10:30:10 | 12 | 13 | 14 \n" +
            "| 2012-01-01 10:30:20 |  22 | 23 | 24 \n";
        setUpContentExpectation(content);

        getDataSource().buildDataset(content, map(
            "type", "xy_line_and_shape",
            "dataset", "timetable_xy",
            "range", "A2-D3",
            "locale", "en_US",
            "date_format", "yyyy-MM-dd kk:mm:ss",
            "domain_axis_type", "date"), null);

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss", new Locale("en"));

        ChartModel chartModel = getDataSource().getChartModel();

        Assert.assertTrue(chartModel.getDataset() instanceof TimeTableXYDataset);

        Assert.assertTrue(chartModel.getAxis(0) instanceof DateAxis);
        Assert.assertTrue(chartModel.getAxis(1) instanceof ValueAxis);

        TimeTableXYDataset dataset = (TimeTableXYDataset) chartModel.getDataset();

        Assert.assertTrue(dataset.getSeriesCount() == 3);

        Assert.assertTrue(dataset.getSeriesKey(0).equals(" column 2 "));
        Assert.assertTrue(dataset.getSeriesKey(1).equals(" column 3 "));
        Assert.assertTrue(dataset.getSeriesKey(2).equals(" column 4"));

        Assert.assertTrue(dataset.getTimePeriod(0).getStart().equals(new Date(0)));
        Assert.assertTrue(dataset.getTimePeriod(0).getEnd().equals(dateFormat.parse("2012-01-01 10:30:10")));

        Assert.assertTrue(dataset.getTimePeriod(1).getStart().equals(dateFormat.parse("2012-01-01 10:30:10")));
        Assert.assertTrue(dataset.getTimePeriod(1).getEnd().equals(dateFormat.parse("2012-01-01 10:30:20")));
    }

    @Test
    public void testYearInterval() throws Exception
    {
        String content =
            "| Date | column 2 | column 3 | column 4\n" +
            "| 1970  | 12 | 13 | 14 \n" +
            "| 1971 |  22 | 23 | 24 \n";
        setUpContentExpectation(content);

        getDataSource().buildDataset(content, map(
            "type", "xy_line_and_shape",
            "dataset", "timetable_xy",
            "range", "A2-D3",
            "locale", "en_US",
            "date_format", "yyyy",
            "domain_axis_type", "date",
            "time_period", "year"), null);

        ChartModel chartModel = getDataSource().getChartModel();

        Assert.assertTrue(chartModel.getDataset() instanceof TimeTableXYDataset);

        Assert.assertTrue(chartModel.getAxis(0) instanceof DateAxis);
        Assert.assertTrue(chartModel.getAxis(1) instanceof ValueAxis);
    }
}
