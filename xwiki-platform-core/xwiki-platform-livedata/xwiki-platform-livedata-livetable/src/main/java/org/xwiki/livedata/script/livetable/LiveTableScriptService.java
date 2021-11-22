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
package org.xwiki.livedata.script.livetable;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableConfiguration;
import org.xwiki.livedata.script.LiveDataScriptService;
import org.xwiki.script.service.ScriptService;

/**
 * Scripting APIs for the Live Data implementation based on Live Table.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(LiveDataScriptService.ROLEHINT + ".liveTable")
@Singleton
public class LiveTableScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    /**
     * Used to convert the live table configuration into live data configuration.
     */
    @Inject
    @Named("liveTable")
    private LiveDataConfigurationResolver<LiveTableConfiguration> liveTableLiveDataConfigResolver;

    /**
     * Used to add the default live data configuration.
     */
    @Inject
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigurationResolver;

    /**
     * Converts the Live Table configuration into Live Data configuration, also adding the default values.
     * 
     * @param id the live table id
     * @param columns the list of live table columns
     * @param columnProperties the column properties
     * @param options the live table options
     * @return the live data configuration
     */
    public LiveDataConfiguration getConfig(String id, List<String> columns, Map<String, Object> columnProperties,
        Map<String, Object> options)
    {
        try {
            LiveTableConfiguration liveTableConfig = new LiveTableConfiguration(id, columns, columnProperties, options);
            // Compute the live data configuration from the live table configuration.
            LiveDataConfiguration liveDataConfig = this.liveTableLiveDataConfigResolver.resolve(liveTableConfig);
            // Add the default values.
            return this.defaultLiveDataConfigurationResolver.resolve(liveDataConfig);
        } catch (LiveDataException e) {
            this.logger.warn(
                "Failed to get live data config for id [{}], columns [{}], "
                    + "columnProperties [{}] and options [{}]. Root cause is [{}].",
                id, columns, columnProperties, options, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
