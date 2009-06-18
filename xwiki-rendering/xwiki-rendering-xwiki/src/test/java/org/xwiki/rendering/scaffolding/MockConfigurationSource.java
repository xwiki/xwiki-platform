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
package org.xwiki.rendering.scaffolding;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Mock {@link org.xwiki.configuration.ConfigurationSourceCollection} that returns an empty list of configuration
 * sources.
 * 
 * @version $Id$
 * @since 1.6M2
 */
public class MockConfigurationSource implements ConfigurationSource
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor<ConfigurationSource> getComponentDescriptor()
    {
        DefaultComponentDescriptor<ConfigurationSource> componentDescriptor = 
            new DefaultComponentDescriptor<ConfigurationSource>();

        componentDescriptor.setRole(ConfigurationSource.class);
        componentDescriptor.setImplementation(MockConfigurationSource.class);

        return componentDescriptor;
    }

    public boolean containsKey(String key)
    {
        return false;
    }

    public List<String> getKeys()
    {
        return Collections.emptyList();
    }

    public <T> T getProperty(String key, Class<T> valueClass)
    {
        return null;
    }

    public <T> T getProperty(String key, T defaultValue)
    {
        return defaultValue;
    }

    public <T> T getProperty(String key)
    {
        return null;
    }

    public boolean isEmpty()
    {
        return true;
    }
}
