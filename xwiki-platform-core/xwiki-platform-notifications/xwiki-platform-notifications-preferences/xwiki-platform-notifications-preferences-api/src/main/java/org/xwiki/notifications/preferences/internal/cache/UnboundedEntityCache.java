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
package org.xwiki.notifications.preferences.internal.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.xwiki.model.reference.EntityReference;

/**
 * Encapsulate a cache.
 * 
 * @param <T> the type of data stored in a cache
 * @version $Id$
 * @since 13.8RC1
 * @since 13.4.4
 * @since 12.10.10
 */
public class UnboundedEntityCache<T>
{
    private final String name;

    private final String jmxName;

    private final boolean invalidateOnUpdate;

    private final Map<EntityReference, T> cache = new ConcurrentHashMap<>();

    /**
     * @param name the name of the name
     * @param invalidateOnUpdate
     */
    public UnboundedEntityCache(String name, boolean invalidateOnUpdate)
    {
        this.name = name;
        this.jmxName = "type=UnboundedEntityCache,name=" + name;
        this.invalidateOnUpdate = invalidateOnUpdate;
    }

    /**
     * @return the name of the name
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @return the name of the JMX bean exposing this cache
     */
    public String getJmxName()
    {
        return this.jmxName;
    }

    /**
     * @return true of the cache entries should be invalidated on document update, false for invalidating them only on
     *         delete
     */
    public boolean isInvalidateOnUpdate()
    {
        return this.invalidateOnUpdate;
    }

    /**
     * @return the cache
     */
    public Map<EntityReference, T> getCache()
    {
        return this.cache;
    }
}
