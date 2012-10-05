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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.xwiki.chart.axis.AxisType;
import org.xwiki.chart.plot.PlotType;
import org.xwiki.rendering.macro.MacroExecutionException;

/**
 * A configurator object for the axes.
 *
 * This super class provides basic parameter validation.
 *
 * @version $Id$
 * @since 4.2M1
 */
public class AxisConfigurator extends AbstractConfigurator
{
    /**
     * The name of the domain axis parameter.
     */
    public static final String DOMAIN_AXIS_PARAM = "domain_axis_type";

    /**
     * The name of the range axis parameter.
     */
    public static final String RANGE_AXIS_PARAM = "range_axis_type";

    /**
     * The name of the domain axis date format parameter.  Used only if the axis type is date.
     */
    public static final String DOMAIN_AXIS_DATE_FORMAT_PARAM = "domain_axis_date_format";

    /**
     * The name of the range axis date format parameter.  Used only if the axis type is date.
     */
    public static final String RANGE_AXIS_DATE_FORMAT_PARAM = "range_axis_date_format";

    /**
     * The lower limit on the domain axis.
     */
    public static final String DOMAIN_AXIS_LOWER_PARAM = "domain_axis_lower";

    /**
     * The upper limit on the domain axis.
     */
    public static final String DOMAIN_AXIS_UPPER_PARAM = "domain_axis_upper";

    /**
     * The lower limit on the range axis.
     */
    public static final String RANGE_AXIS_LOWER_PARAM = "range_axis_lower";

    /**
     * The upper limit on the range axis.
     */
    public static final String RANGE_AXIS_UPPER_PARAM = "range_axis_upper";

    /**
     * The configured axis types.
     */
    private AxisType[] axisTypes = {null, null, null};

    /**
     * The configured axis date formats.
     */
    private String[] axisDateFormat = {null, null, null};

    /**
     * The locale configuration.
     */
    private final LocaleConfiguration localeConfiguration;

    /**
     * The lower limits.
     */
    private String[] axisLowerLimit = {null, null, null};

    /**
     * The upper limits.
     */
    private String[] axisUpperLimit = {null, null, null};

    /**
     * @param localeConfiguration The locale configuration.
     */
    public AxisConfigurator(LocaleConfiguration localeConfiguration)
    {
        this.localeConfiguration = localeConfiguration;
    }

    @Override
    public boolean setParameter(String key, String value) throws MacroExecutionException
    {
        boolean claimed = true;

        if (DOMAIN_AXIS_PARAM.equals(key)) {
            AxisType type = AxisType.forName(value);
            if (type == null) {
                invalidParameterValue(DOMAIN_AXIS_PARAM, value);
            }
            setAxisType(0, type);
        } else if (DOMAIN_AXIS_DATE_FORMAT_PARAM.equals(key)) {
            setAxisDateFormat(0, value);
        } else if (RANGE_AXIS_PARAM.equals(key)) {
            AxisType type = AxisType.forName(value);
            if (type == null) {
                invalidParameterValue(RANGE_AXIS_PARAM, value);
            }
            setAxisType(1, type);
        } else if (RANGE_AXIS_DATE_FORMAT_PARAM.equals(key)) {
            setAxisDateFormat(0, value);
        } else if (!claimLimitParameters(key, value)) {
            claimed = false;
        }

        return claimed;
    }

    /**
     * Claime limit parameters.
     *
     * @param key the parameter name.
     * @param value the parameter value.
     * @return {@code true} if the parameter was claimed.
     */
    private boolean claimLimitParameters(String key, String value)
    {
        boolean claimed = true;

        if (DOMAIN_AXIS_LOWER_PARAM.equals(key)) {
            axisLowerLimit[0] = value;
        } else if (DOMAIN_AXIS_UPPER_PARAM.equals(key)) {
            axisUpperLimit[0] = value;
        } else if (RANGE_AXIS_LOWER_PARAM.equals(key)) {
            axisLowerLimit[1] = value;
        } else if (RANGE_AXIS_UPPER_PARAM.equals(key)) {
            axisUpperLimit[1] = value;
        } else {
            claimed = false;
        }

        return claimed;
    }

    /**
     * Set the axis type for the axis at the given index.
     *
     * @param index The index.
     * @param axisType the axis type.
     */
    private void setAxisType(int index, AxisType axisType)
    {
        axisTypes[index] = axisType;
    }

    /**
     * Set the date format for the axis at the given index.
     *
     * @param index The index.
     * @param dateFormatString the date format.
     * @throws MacroExecutionException if the date format string is invalid.
     */
    private void setAxisDateFormat(int index, String dateFormatString) throws MacroExecutionException
    {
        axisDateFormat[index] = dateFormatString;
    }

    /**
     * Set the axes in the chart model.
     *
     * @param plotType The target plot type.
     * @param chartModel The target chart model.
     * @throws MacroExecutionException if the axes are incorrectly configured.
     */
    public void setAxes(PlotType plotType, SimpleChartModel chartModel) throws MacroExecutionException
    {
        AxisType[] defaultAxisTypes = plotType.getDefaultAxisTypes();

        for (int i = 0; i < axisTypes.length; i++) {
            AxisType type = axisTypes[i];
            if (i >= defaultAxisTypes.length) {
                if (type != null) {
                    throw new MacroExecutionException("To many axes for plot type.");
                }
                continue;
            }
            if (type == null) {
                type = defaultAxisTypes[i];
            }

            Axis axis;

            switch (type) {
                case NUMBER:
                    NumberAxis numberAxis = new NumberAxis();
                    axis = numberAxis;
                    setNumberLimits(numberAxis, i);
                    break;
                case CATEGORY:
                    axis = new CategoryAxis();
                    break;
                case DATE:
                    DateAxis dateAxis = new DateAxis();
                    axis = dateAxis;
                    dateAxis.setTickMarkPosition(DateTickMarkPosition.END);
                    if (axisDateFormat[i] != null) {
                        try {
                            DateFormat dateFormat = new SimpleDateFormat(axisDateFormat[i],
                                localeConfiguration.getLocale());
                            dateAxis.setDateFormatOverride(dateFormat);
                        } catch (IllegalArgumentException e) {
                            throw new MacroExecutionException(String.format("Invalid date format [%s].",
                                axisDateFormat[i]));
                        }
                    }
                    setDateLimits(dateAxis, i);
                    break;
                default:
                    throw new MacroExecutionException(String.format("Unsupported axis type [%s]", type.getName()));
            }

            chartModel.setAxis(i, axis);
        }
    }

    /**
     * Set the limits of a number axis.
     *
     * @param axis The axis.
     * @param index The index of the axis
     * @throws MacroExecutionException if the parameters could not be parsed as numbers.
     */
    private void setNumberLimits(ValueAxis axis, int index) throws MacroExecutionException
    {
        try {
            if (axisLowerLimit[index] != null) {
                Number number = NumberUtils.createNumber(StringUtils.trim(axisLowerLimit[index]));
                axis.setLowerBound(number.doubleValue());
            }
            if (axisUpperLimit[index] != null) {
                Number number = NumberUtils.createNumber(StringUtils.trim(axisUpperLimit[index]));
                axis.setUpperBound(number.doubleValue());
            }
        } catch (NumberFormatException e) {
            throw new MacroExecutionException("Invalid number in axis bound.", e);
        }
    }

    /**
     * Set the limits of a date axis.
     *
     * @param axis The axis.
     * @param index The index of the axis.
     * @throws MacroExecutionException if the parameters could not be parsed as dates.
     */
    private void setDateLimits(DateAxis axis, int index) throws MacroExecutionException
    {
        try {
            if (axisLowerLimit[index] != null) {
                Date date = localeConfiguration.getDateFormat().parse(StringUtils.trim(axisLowerLimit[index]));
                axis.setMinimumDate(date);
            }
            if (axisUpperLimit[index] != null) {
                Date date = localeConfiguration.getDateFormat().parse(StringUtils.trim(axisUpperLimit[index]));
                axis.setMaximumDate(date);
            }
        } catch (ParseException e) {
            throw new MacroExecutionException("Invalid date in axis bound.", e);
        }
    }
}
