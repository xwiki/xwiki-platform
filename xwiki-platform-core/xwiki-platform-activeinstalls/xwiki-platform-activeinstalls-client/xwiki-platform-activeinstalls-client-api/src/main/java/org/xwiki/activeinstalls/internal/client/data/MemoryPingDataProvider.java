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
package org.xwiki.activeinstalls.internal.client.data;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.activeinstalls.internal.client.PingDataProvider;
import org.xwiki.component.annotation.Component;

/**
 * Provide memory related informations (max memory, allocated memory, etc).
 *
 * @version $Id$
 * @since 8.3M1
 */
@Component
@Named("memory")
@Singleton
public class MemoryPingDataProvider implements PingDataProvider
{
    private static final String PROPERTY_MAX_MEMORY = "maxMemory";

    private static final String PROPERTY_TOTAL_MEMORY = "totalMemory";

    private static final String PROPERTY_FREE_MEMORY = "freeMemory";

    private static final String PROPERTY_USED_MEMORY = "usedMemory";

    @Override
    public Map<String, Object> provideMapping()
    {
        Map<String, Object> map = new HashMap<>();
        map.put("type", "long");

        Map<String, Object> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_MAX_MEMORY, map);
        propertiesMap.put(PROPERTY_TOTAL_MEMORY, map);
        propertiesMap.put(PROPERTY_FREE_MEMORY, map);
        propertiesMap.put(PROPERTY_USED_MEMORY, map);

        return propertiesMap;
    }

    @Override
    public Map<String, Object> provideData()
    {
        Map<String, Object> jsonMap = new HashMap<>();
        jsonMap.put(PROPERTY_MAX_MEMORY, Runtime.getRuntime().maxMemory());
        jsonMap.put(PROPERTY_TOTAL_MEMORY, Runtime.getRuntime().totalMemory());
        jsonMap.put(PROPERTY_FREE_MEMORY, Runtime.getRuntime().freeMemory());
        jsonMap.put(PROPERTY_USED_MEMORY, Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        return jsonMap;
    }
}
