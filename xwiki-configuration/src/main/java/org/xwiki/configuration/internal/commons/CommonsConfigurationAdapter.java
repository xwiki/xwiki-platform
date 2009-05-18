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

import org.apache.commons.configuration.AbstractConfiguration;
import org.xwiki.configuration.ConfigurationSource;

import java.util.Iterator;

/**
 * Adapter to wrap a XWiki {@link org.xwiki.configuration.ConfigurationSource} into
 * a Commons Configuration instance. We need this in
 * {@link org.xwiki.configuration.internal.DefaultConfigurationManager} since we're using a
 * Commons Configuration {@link org.apache.commons.configuration.CompositeConfiguration} isntance
 * to manager multiple source configurations.
 *  
 * @version $Id$
 * @since 1.6M1
 */
public class CommonsConfigurationAdapter extends AbstractConfiguration
{
    private ConfigurationSource source;

    public CommonsConfigurationAdapter(ConfigurationSource source)
    {
        super();
        this.source = source;
    }

    protected void addPropertyDirect(String s, Object o)
    {
        // Do nothing.
    }

    public Iterator getKeys()
    {
        return this.source.getKeys().iterator();
    }

    public Object getProperty(String key)
    {
        return this.source.getProperty(key);
    }

    public boolean containsKey(String key)
    {
        return this.source.containsKey(key);
    }

    public boolean isEmpty()
    {
        return this.source.isEmpty();
    }
}
