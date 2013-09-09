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
package org.xwiki.wikistream.internal;

import java.util.Map;

import org.xwiki.filter.FilterEventParameters;

/**
 * 
 * @version $Id$
 * @since 5.2M2
 */
public class ParametersTree
{
    private final ParametersTree parent;

    private final FilterEventParameters properties;

    public ParametersTree(FilterEventParameters properties, ParametersTree parent)
    {
        this.properties = properties != null ? properties : FilterEventParameters.EMPTY;
        this.parent = parent;
    }

    public ParametersTree getParent()
    {
        return this.parent;
    }

    public Map<String, Object> getProperties()
    {
        return this.properties;
    }

    public <T> T get(String key)
    {
        return get(key, null);
    }

    public <T> T get(String key, T def)
    {
        if (getProperties().containsKey(key)) {
            return (T) this.properties.get(key);
        }

        if (getParent() != null) {
            return getParent().get(key, def);
        }

        return def;
    }

    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();

        builder.append(this.properties.toString());

        if (this.parent != null) {
            builder.append(" -> ");
            builder.append(this.parent);
        }

        return builder.toString();
    }
}
