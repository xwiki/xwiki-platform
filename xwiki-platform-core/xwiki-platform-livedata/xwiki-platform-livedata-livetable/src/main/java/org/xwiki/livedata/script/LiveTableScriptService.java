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
package org.xwiki.livedata.script;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.internal.script.LiveTableConfigHelper;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * Scripting APIs for the Live Data implementation based on Live Table.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component
@Named(LiveDataScriptService.ROLEHINT + ".liveTable")
@Singleton
@Unstable
public class LiveTableScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    private LiveTableConfigHelper configHelper;

    /**
     * Converts the Live Table configuration into Live Data configuration.
     * 
     * @param id the live table id
     * @param columns the list of live table columns
     * @param columnProperties the column properties
     * @param options the live table options
     * @return the live data configuration
     */
    public Map<String, Object> getConfig(String id, List<String> columns, Map<String, Object> columnProperties,
        Map<String, Object> options)
    {
        try {
            return this.configHelper.getConfig(id, columns, columnProperties, options);
        } catch (Exception e) {
            this.logger.warn(
                "Failed to get live data config for id [{}], columns [{}], "
                    + "columnProperties [{}] and options [{}]. Root cause is [{}].",
                id, columns, columnProperties, options, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Converts the Live Table configuration into Live Data configuration.
     * 
     * @param id the live table id
     * @param columns the list of live table columns
     * @param columnProperties the column properties
     * @param options the live table options
     * @return the live data configuration JSON
     */
    public String getConfigJSON(String id, List<String> columns, Map<String, Object> columnProperties,
        Map<String, Object> options)
    {
        try {
            return this.configHelper.getConfigJSON(id, columns, columnProperties, options);
        } catch (Exception e) {
            this.logger.warn(
                "Failed to get live data config JSON for id [{}], columns [{}],"
                    + " columnProperties [{}] and options [{}]. Root cause is [{}].",
                id, columns, columnProperties, options, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }
}
