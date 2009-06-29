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
package org.xwiki.configuration.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.xwiki.configuration.ConfigurationSource;

/**
 * Allows composing (aka chaining) several Configuration Sources. The order of sources is important. 
 * Sources located before other sources take priority.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class CompositeConfigurationSource implements ConfigurationSource
{
    /**
     * The order of sources is important. Sources located before other sources take priority.
     */
    private List<ConfigurationSource> sources = new ArrayList<ConfigurationSource>();

    public void addConfigurationSource(ConfigurationSource source)
    {
        this.sources.add(source);
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#containsKey(String)
     */
    public boolean containsKey(String key)
    {
        boolean result = false;
        for (ConfigurationSource source : this.sources) {
            if (source.containsKey(key)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getProperty(String)
     */
    public <T> T getProperty(String key)
    {
        T result = null;
        for (ConfigurationSource source : this.sources) {
            if (source.containsKey(key)) {
                result = source.<T>getProperty(key); 
                break;
            }
        }
        return result;
    }
    
    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getProperty(String, Class)
     */
    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T result = null;
        for (ConfigurationSource source : this.sources) {
            if (source.containsKey(key)) {
                result = source.getProperty(key, valueClass);
                break;
            }
        }
        // List and Properties must return empty collections and not null values.
        if (result == null) {
            if (List.class.getName().equals(valueClass.getName())) {
                result = (T) Collections.emptyList();
            } else if (Properties.class.getName().equals(valueClass.getName())) {
                result = (T) new Properties();
            }
        }
        
        return result;
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getProperty(String, Object)
     */
    public <T> T getProperty(String key, T defaultValue)
    {
        T result = null;
        for (ConfigurationSource source : this.sources) {
            if (source.containsKey(key)) {
                result = source.<T>getProperty(key);
                break;
            }
        }
        if (result == null) {
            result = defaultValue;
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#getKeys()
     */
    public List<String> getKeys()
    {
        // We use a linked hash set in order to keep the keys in the order in which they were defined in the sources.
        Set<String> keys = new LinkedHashSet<String>();
        for (ConfigurationSource source : this.sources) {
            keys.addAll(source.getKeys());
        }
        return new ArrayList<String>(keys);
    }

    /**
     * {@inheritDoc}
     * @see ConfigurationSource#isEmpty()
     */
    public boolean isEmpty()
    {
        boolean result = true;
        for (ConfigurationSource source : this.sources) {
            if (!source.isEmpty()) {
                result = false;
                break;
            }
        }            
        return result;
    }
}
