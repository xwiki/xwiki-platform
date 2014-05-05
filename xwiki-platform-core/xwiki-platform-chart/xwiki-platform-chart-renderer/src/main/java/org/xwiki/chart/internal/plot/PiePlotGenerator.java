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

import java.text.NumberFormat;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.PieDataset;
import org.xwiki.chart.PlotGeneratorException;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.component.annotation.Component;

/**
 * A {@link PlotGenerator} for generating pie charts.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named("pie")
@Singleton
public class PiePlotGenerator implements PlotGenerator
{
    private static final String PIE_LABEL_FORMAT_KEY = "pie_label_format";

    @Override
    public Plot generate(ChartModel model, Map<String, String> parameters) throws PlotGeneratorException
    {
        PieDataset dataset;
        try {
            dataset = (PieDataset) model.getDataset();
        } catch (ClassCastException e) {
            throw new PlotGeneratorException("Incompatible dataset for the pie plot.");
        }
        PiePlot piePlot = new PiePlot(dataset);

        // Allow customizing the label to use
        String pieLabelFormat = parameters.get(PIE_LABEL_FORMAT_KEY);
        if (pieLabelFormat != null) {
            piePlot.setLabelGenerator(
                new StandardPieSectionLabelGenerator(pieLabelFormat, NumberFormat.getNumberInstance(),
                    NumberFormat.getPercentInstance()));
        }

        return piePlot;
    }
}
