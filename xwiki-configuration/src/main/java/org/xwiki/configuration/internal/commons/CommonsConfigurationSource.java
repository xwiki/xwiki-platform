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
package org.xwiki.configuration.internal.commons;

import org.xwiki.configuration.ConfigurationSource;
import org.apache.commons.configuration.Configuration;

import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Wrap a Commons Configuration instance into a XWiki {@link ConfigurationSource}. This allows
 * us to reuse the <a href= "http://commons.apache.org/configuration/"numerous types of
 * Configuration<a/> provided by Commons Configuration (properties file, XML files, databases, etc).
 *
 * @version $Id$
 * @since 1.6M1
 */
public class CommonsConfigurationSource implements ConfigurationSource
{
    private Configuration configuration;

    public CommonsConfigurationSource(Configuration configuration)
    {
        this.configuration = configuration;    
    }

    public Object getProperty(String name)
    {
        return this.configuration.getProperty(name);
    }

    public List<String> getKeys()
    {
        List<String> keysList = new ArrayList<String>();
        Iterator keys = this.configuration.getKeys();
        while (keys.hasNext()) {
            keysList.add((String) keys.next());
        }
        return keysList;
    }

    public boolean containsKey(String key)
    {
        return this.configuration.containsKey(key);
    }

    public boolean isEmpty()
    {
        return this.configuration.isEmpty();
    }
}
