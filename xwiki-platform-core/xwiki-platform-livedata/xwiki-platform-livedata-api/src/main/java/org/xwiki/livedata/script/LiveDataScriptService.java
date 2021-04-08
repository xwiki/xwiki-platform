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

import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.internal.script.LiveDataConfigHelper;
import org.xwiki.script.service.ScriptService;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.stability.Unstable;

/**
 * Scripting APIs for the Live Data component.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(LiveDataScriptService.ROLEHINT)
@Singleton
@Unstable
public class LiveDataScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLEHINT = "liveData";

    @Inject
    private Logger logger;

    @Inject
    private LiveDataSourceManager sourceManager;

    @Inject
    private LiveDataConfigHelper configHelper;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    /**
     * Executes a live data query.
     * 
     * @param queryConfig the live data query configuration
     * @return the live data entries that match the given query
     */
    public LiveData query(Map<String, Object> queryConfig)
    {
        try {
            return query(this.configHelper.createQuery(queryConfig));
        } catch (Exception e) {
            this.logger.warn("Failed to create the live data query from [{}]. Root cause is [{}].", queryConfig,
                ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Executes a live data query.
     * 
     * @param queryConfigJSON the live data query configuration
     * @return the live data entries that match the given query
     */
    public LiveData query(String queryConfigJSON)
    {
        try {
            return query(this.configHelper.createQuery(queryConfigJSON));
        } catch (Exception e) {
            this.logger.warn("Failed to create the live data query from JSON [{}]. Root cause is [{}].",
                queryConfigJSON, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    private LiveData query(LiveDataQuery query)
    {
        Optional<LiveDataSource> source = this.sourceManager.get(query.getSource());
        if (source.isPresent()) {
            try {
                return source.get().getEntries().get(query);
            } catch (LiveDataException e) {
                this.logger.warn("Failed to execute live data query. Root cause is [{}].",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return null;
    }

    /**
     * Computes the effective live data configuration by normalizing the given configuration (i.e. transforming it to
     * match the format expected by the live data widget) and adding the (missing) default values.
     * 
     * @param liveDataConfig the live data configuration to start with
     * @return the effective live data configuration, using the standard format and containing the default values
     */
    public Map<String, Object> effectiveConfig(Map<String, Object> liveDataConfig)
    {
        try {
            return this.configHelper.effectiveConfig(liveDataConfig);
        } catch (Exception e) {
            this.logger.warn("Failed to compute the effective live data configuration for [{}]. Root cause is [{}].",
                liveDataConfig, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Computes the effective live data configuration by normalizing the given configuration (i.e. transforming it to
     * match the format expected by the live data widget) and adding the (missing) default values.
     * 
     * @param liveDataConfigJSON the live data configuration to start with
     * @return the effective live data configuration, using the standard format and containing the default values
     */
    public String effectiveConfig(String liveDataConfigJSON)
    {
        try {
            return this.configHelper.effectiveConfig(liveDataConfigJSON);
        } catch (Exception e) {
            this.logger.warn(
                "Failed to compute the effective live data configuration for JSON [{}]. Root cause is [{}].",
                liveDataConfigJSON, ExceptionUtils.getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * @param <S> the type of the {@link ScriptService}
     * @param serviceName the name of the sub {@link ScriptService}
     * @return the {@link ScriptService} or null of none could be found
     */
    @SuppressWarnings("unchecked")
    public <S extends ScriptService> S get(String serviceName)
    {
        return (S) this.scriptServiceManager.get(ROLEHINT + '.' + serviceName);
    }
}
