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
package com.xpn.xwiki.plugin.charts.plots;

import org.jfree.data.xy.DefaultTableXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;

import com.xpn.xwiki.plugin.charts.exceptions.DataSourceException;
import com.xpn.xwiki.plugin.charts.exceptions.GenerateException;
import com.xpn.xwiki.plugin.charts.params.ChartParams;
import com.xpn.xwiki.plugin.charts.source.DataSource;

public class TableXYDatasetFactory
{
    private static TableXYDatasetFactory uniqueInstance = new TableXYDatasetFactory();

    private TableXYDatasetFactory()
    {
        // empty
    }

    public static TableXYDatasetFactory getInstance()
    {
        return uniqueInstance;
    }

    public XYDataset create(DataSource dataSource, ChartParams params) throws GenerateException, DataSourceException
    {
        // String type = params.getStringParam("type");
        String dataSeries = params.getString(ChartParams.SERIES);

        DefaultTableXYDataset dataset = new DefaultTableXYDataset();
        if (dataSeries.equals("columns")) {
            if (!dataSource.hasHeaderColumn()) {
                throw new GenerateException("Header column required");
            }
            for (int column = 0; column < dataSource.getColumnCount(); column++) {
                XYSeries series =
                    new XYSeries(dataSource.hasHeaderRow() ? dataSource.getHeaderRowValue(column)
                        : ("Series " + (column + 1)), false, false);
                for (int row = 0; row < dataSource.getRowCount(); row++) {
                    series.add(Double.parseDouble(dataSource.getHeaderColumnValue(row)), dataSource
                        .getCell(row, column));
                }
                dataset.addSeries(series);
            }

        } else if (dataSeries.equals("rows")) {
            if (!dataSource.hasHeaderRow()) {
                throw new GenerateException("Header row required");
            }
            for (int row = 0; row < dataSource.getRowCount(); row++) {
                XYSeries series =
                    new XYSeries(dataSource.hasHeaderColumn() ? dataSource.getHeaderColumnValue(row)
                        : ("Series " + (row + 1)), false, false);
                for (int column = 0; column < dataSource.getColumnCount(); column++) {
                    series.add(Double.parseDouble(dataSource.getHeaderRowValue(column)), dataSource
                        .getCell(row, column));
                }
                dataset.addSeries(series);
            }
        } else {
            throw new GenerateException("Invalid series parameter:" + dataSeries);
        }
        return dataset;
    }
}
