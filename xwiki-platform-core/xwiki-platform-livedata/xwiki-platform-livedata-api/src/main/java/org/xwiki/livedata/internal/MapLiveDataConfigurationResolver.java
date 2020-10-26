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
package org.xwiki.livedata.internal;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Resolves the live data configuration from a string map.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Singleton
public class MapLiveDataConfigurationResolver implements LiveDataConfigurationResolver<Map<String, Object>>
{
    @Inject
    private LiveDataConfigurationResolver<String> stringConfigResolver;

    @Override
    public LiveDataConfiguration resolve(Map<String, Object> input) throws LiveDataException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return this.stringConfigResolver.resolve(objectMapper.writeValueAsString(input));
        } catch (JsonProcessingException e) {
            throw new LiveDataException(e);
        }
    }
}
