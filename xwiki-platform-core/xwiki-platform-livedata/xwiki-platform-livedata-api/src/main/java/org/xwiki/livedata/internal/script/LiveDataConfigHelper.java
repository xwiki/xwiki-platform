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
package org.xwiki.livedata.internal.script;

import java.io.IOException;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataQuery;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Default implementation of {@link LiveDataConfigHelper}.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component(roles = LiveDataConfigHelper.class)
@Singleton
public class LiveDataConfigHelper
{

    @Inject
    private LiveDataConfigurationResolver<String> stringConfigResolver;

    @Inject
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultConfigResolver;

    /**
     * Creates a live data query based on the given configuration.
     * 
     * @param queryConfig the live data query configuration
     * @return the live data query instance
     * @throws Exception if the given query configuration cannot be serialized as JSON
     */
    public LiveDataQuery createQuery(Map<String, Object> queryConfig) throws Exception
    {
        return createQuery(new ObjectMapper().writeValueAsString(queryConfig));
    }

    /**
     * Creates a live data query based on the given configuration.
     * 
     * @param queryConfigJSON the live data query configuration
     * @return the live data query instance
     * @throws Exception if the given configuration cannot be parsed as JSON or if it cannot be converted into a
     *             {@link LiveDataQuery} instance
     */
    public LiveDataQuery createQuery(String queryConfigJSON) throws Exception
    {
        String liveDataConfigJSON = "{\"query\": " + queryConfigJSON + "}";
        LiveDataConfiguration liveDataConfig =
            this.defaultConfigResolver.resolve(this.stringConfigResolver.resolve(liveDataConfigJSON));
        return liveDataConfig.getQuery();
    }

    /**
     * Computes the effective live data configuration by normalizing the given configuration (i.e. transforming it to
     * match the format expected by the live data widget) and adding the (missing) default values.
     * 
     * @param liveDataConfig the live data configuration to start with
     * @return the effective live data configuration, using the standard format and containing the default values
     * @throws IOException if the given live data configuration cannot be serialized as JSON
     */
    public Map<String, Object> effectiveConfig(Map<String, Object> liveDataConfig) throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String effectiveConfigJSON = effectiveConfig(objectMapper.writeValueAsString(liveDataConfig));
        return objectMapper.readerForMapOf(Object.class).readValue(effectiveConfigJSON);
    }

    /**
     * Computes the effective live data configuration by normalizing the given configuration (i.e. transforming it to
     * match the format expected by the live data widget) and adding the (missing) default values.
     * 
     * @param liveDataConfigJSON the live data configuration to start with
     * @return the effective live data configuration, using the standard format and containing the default values
     * @throws Exception if it fails to parse the live data configuration JSON
     */
    public String effectiveConfig(String liveDataConfigJSON) throws Exception
    {
        LiveDataConfiguration liveDataConfig =
            this.defaultConfigResolver.resolve(this.stringConfigResolver.resolve(liveDataConfigJSON));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        return objectMapper.writeValueAsString(liveDataConfig);
    }
}
