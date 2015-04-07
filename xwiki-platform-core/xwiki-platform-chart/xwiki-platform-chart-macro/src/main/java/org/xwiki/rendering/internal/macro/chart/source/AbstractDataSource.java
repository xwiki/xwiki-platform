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

import java.util.Map;

import org.jfree.data.general.Dataset;
import org.xwiki.chart.dataset.DatasetType;
import org.xwiki.chart.model.ChartModel;
import org.xwiki.chart.plot.PlotType;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * A data source is able to provide a data set for chart generation.
 *
 * This super class provides basic parameter validation.
 *
 * @version $Id$
 * @since 4.2M1
 */
public abstract class AbstractDataSource implements DataSource
{
    /**
     * The name of the dataset parameter.
     */
    public static final String DATASET_PARAM = "dataset";

    /**
     * The name of the plot type parameter.
     */
    public static final String PLOT_TYPE_PARAM = "type";

    /**
     * The dataset type.
     */
    private DatasetType datasetType;

    /**
     * The plot type.
     */
    private PlotType plotType;

    /**
     * The built chart model.
     */
    private SimpleChartModel chartModel;

    /**
     * A configuration object for time zone and locale.
     */
    private LocaleConfiguration localeConfiguration = new LocaleConfiguration();

    /**
     * An axis configurator.
     */
    private AxisConfigurator axisConfigurator = new AxisConfigurator(localeConfiguration);

    /**
     * Validate and set parameters for the data source.
     *
     * @param parameters The parameters.
     * @throws MacroExecutionException if the parameters are invalid for this data source.
     */
    protected void validateParameters(Map<String, String> parameters) throws MacroExecutionException
    {
        for (String key : parameters.keySet()) {
            if (DATASET_PARAM.equals(key)) {
                datasetType = DatasetType.forName(parameters.get(key));
                if (datasetType == null) {
                    invalidParameterValue(DATASET_PARAM, parameters.get(key));
                }
                continue;
            }
            if (PLOT_TYPE_PARAM.equals(key)) {
                plotType = PlotType.forName(parameters.get(key));
                if (plotType == null) {
                    invalidParameterValue(PLOT_TYPE_PARAM, parameters.get(key));
                }
                continue;
            }
            if (localeConfiguration.setParameter(key, parameters.get(key))) {
                continue;
            }
            if (axisConfigurator.setParameter(key, parameters.get(key))) {
                continue;
            }
            setParameter(key, parameters.get(key));
        }

        localeConfiguration.validateParameters();

        axisConfigurator.validateParameters();

        validatePlotType();

        validateDatasetType();

        validateParameters();
    }

    /**
     * Let an implementation set a parameter.
     *
     * This method should set the value of the parameter, if the parameter is supported by the data source.
     *
     * @param key The key of the parameter.
     * @param value The value of the parameter.
     * @return {@code true} if the parameter was claimed.
     * @throws MacroExecutionException if the parameter is invalid in some way.
     */
    protected abstract boolean setParameter(String key, String value) throws MacroExecutionException;

    /**
     * Let an implementation validate the value of the previously set parameters, and set default values.
     *
     * @throws MacroExecutionException if the previously set value is invalid.
     */
    protected abstract void validateParameters() throws MacroExecutionException;

    /**
     * Validate the dataset parameter.  If no dataset was given as a parameter, set a default value.
     *
     * @throws MacroExecutionException if the previously set value is invalid.
     */
    protected void validateDatasetType() throws MacroExecutionException
    {
        if (getDatasetType() == null) {
            setDatasetType(plotType.getDefaultDatasetType());
        }
    }

    /**
     * Validate the plot type parameter.
     *
     * @throws MacroExecutionException if the previously set value is invalid.
     */
    protected void validatePlotType() throws MacroExecutionException
    {
        if (plotType == null) {
            throw new MacroExecutionException(String.format("The parameter [%s] is mandatory!", PLOT_TYPE_PARAM));
        }
    }

    /**
     * Set the axes from the axis configuration.
     *
     * @throws MacroExecutionException if the axes are incorrectly specified.
     */
    protected void setAxes() throws MacroExecutionException
    {
        axisConfigurator.setAxes(plotType, chartModel);
    }

    /**
     * @return the configured dataset type.
     */
    public DatasetType getDatasetType()
    {
        return this.datasetType;
    }

    /**
     * @param datasetType the dataset type to configure.
     */
    public void setDatasetType(DatasetType datasetType)
    {
        this.datasetType = datasetType;
    }

    @Override
    public ChartModel getChartModel()
    {
        return chartModel;
    }

    /**
     * Set the chart model.
     *
     * @param chartModel The chart model.
     */
    protected void setChartModel(SimpleChartModel chartModel)
    {
        this.chartModel = chartModel;
    }

    /**
     * @param dataset the dataset.
     */
    protected void setDataset(Dataset dataset)
    {
        chartModel.setDataset(dataset);
    }

    /**
     * Indicate that an invalid parameter value was found.
     *
     * @param parameterName The name of the parameter.
     * @param value The value.
     * @throws MacroExecutionException always.
     */
    protected void invalidParameterValue(String parameterName, String value) throws MacroExecutionException
    {
        throw new MacroExecutionException(String.format("Invalid value for parameter [%s]: [%s]",
            parameterName, value));
    }

    /**
     * @return The locale configuration.
     */
    protected LocaleConfiguration getLocaleConfiguration()
    {
        return localeConfiguration;
    }
}
