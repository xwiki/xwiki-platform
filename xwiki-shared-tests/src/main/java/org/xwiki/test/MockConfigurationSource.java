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
package org.xwiki.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Mock {@link ConfigurationSource} that returns an empty list of configuration sources.
 * 
 * @version $Id$
 * @since 1.6M2
 */
public class MockConfigurationSource implements ConfigurationSource
{
    private Map<String, Object> properties = new HashMap<String, Object>();

    public static DefaultComponentDescriptor<ConfigurationSource> getDescriptor(String roleHint)
    {
        DefaultComponentDescriptor<ConfigurationSource> descriptor =
            new DefaultComponentDescriptor<ConfigurationSource>();
        descriptor.setRole(ConfigurationSource.class);
        if (roleHint != null) {
            descriptor.setRoleHint(roleHint);
        }
        descriptor.setImplementation(MockConfigurationSource.class);

        return descriptor;
    }

    public void setProperty(String key, Object value)
    {
        this.properties.put(key, value);
    }

    public void removeProperty(String key)
    {
        this.properties.remove(key);
    }

    public boolean containsKey(String key)
    {
        return this.properties.containsKey(key);
    }

    public List<String> getKeys()
    {
        return new ArrayList<String>(this.properties.keySet());
    }

    public <T> T getProperty(String key, Class<T> valueClass)
    {
        T result = null;

        if (this.properties.containsKey(key)) {
            result = (T) this.properties.get(key);
        } else {
            if (List.class.getName().equals(valueClass.getName())) {
                result = (T) Collections.emptyList();
            } else if (Properties.class.getName().equals(valueClass.getName())) {
                result = (T) new Properties();
            }
        }

        return result;
    }

    public <T> T getProperty(String key, T defaultValue)
    {
        return this.properties.containsKey(key) ? (T) this.properties.get(key) : defaultValue;
    }

    public <T> T getProperty(String key)
    {
        return (T) this.properties.get(key);
    }

    public boolean isEmpty()
    {
        return this.properties.isEmpty();
    }
}
