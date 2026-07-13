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
package org.xwiki.chart.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * Component in charge of loading configuration information for the chart module.
 *
 * @version $Id$
 * @since 18.6.0RC1
 * @since 18.4.3
 * @since 17.10.11
 */
@Component(roles = ChartConfiguration.class)
@Singleton
public class ChartConfiguration
{
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    /**
     * @return the maximum height of a chart.
     */
    public int getMaximumChartHeight()
    {
        return this.configurationSource.getProperty("chart.height.max", 2048);
    }

    /**
     * @return the maximum width of a chart.
     */
    public int getMaximumChartWidth()
    {
        return this.configurationSource.getProperty("chart.width.max", 2048);
    }
}
