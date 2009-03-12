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
 *
 */
package org.xwiki.context;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

/**
 * Contains all state data related to the current user action. Note that the execution context is independent of the
 * environment and all environment-dependent data are stored in the Container component instead.
 *
 * @version $Id$ 
 * @since 1.5M2
 */
public class ExecutionContext
{
    private Map<String, Object> properties = new HashMap<String, Object>();

    /**
     * @param key the key under which is stored the property to retrieve
     * @return the property matching the passed key
     */
    public Object getProperty(String key)
    {
        return this.properties.get(key);
    }

    /**
     * @return all the context properties
     */
    public Map<String, Object> getProperties()
    {
        return Collections.unmodifiableMap(this.properties);
    }
    
    /**
     * @param key remove the property whose key matches the passed key
     */
    public void removeProperty(String key)
    {
        this.properties.remove(key);
    }

    /**
     * @param key the key under which to save the passed property value
     * @param value the value to set
     */
    public void setProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }

    /**
     * @param properties the properties to add to the context
     */
    public void setProperties(Map<String, Object> properties)
    {
        this.properties.putAll(properties);
    }
}
