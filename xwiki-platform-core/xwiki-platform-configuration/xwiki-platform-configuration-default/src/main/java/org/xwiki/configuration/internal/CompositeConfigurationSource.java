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
import java.util.Iterator;
import java.util.List;

import org.xwiki.configuration.ConfigurationSource;

/**
 * Allows composing (aka chaining) several Configuration Sources. The order of sources is important. Sources located
 * before other sources take priority.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class CompositeConfigurationSource extends AbstractCompositeConfigurationSource
{
    /**
     * The order of sources is important. Sources located before other sources take priority.
     */
    private List<ConfigurationSource> sources = new ArrayList<ConfigurationSource>();

    public void addConfigurationSource(ConfigurationSource source)
    {
        this.sources.add(source);
    }

    @Override
    public Iterator<ConfigurationSource> iterator()
    {
        return this.sources.iterator();
    }
}
