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
package org.xwiki.chart.internal.plot;

import java.util.Map;

import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.DefaultPieDataset;
import org.xwiki.chart.model.ChartModel;

/**
 * A {@link PlotGenerator} for generating pie charts.
 *
 * @version $Id$
 * @since 2.0M1
 */
public class PiePlotGenerator implements PlotGenerator
{
    @Override
    public Plot generate(ChartModel model, Map<String, String> parameters)
    {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String dataSeries = parameters.get("series");
        if (dataSeries.equals("rows")) {
            for (int column = 0; column < model.getColumnCount(); column++) {
                String category = model.getColumnHeader(column);
                dataset.setValue(category, model.getCellValue(0, column));
            }
        } else {
            for (int row = 0; row < model.getRowCount(); row++) {
                String category = model.getRowHeader(row);
                dataset.setValue(category, model.getCellValue(row, 0));
            }
        }
        return new PiePlot(dataset);
    }
}
