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
package org.xwiki.wikistream.instance.internal.input;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.properties.RawProperties;
import org.xwiki.stability.Unstable;

/**
 * @version $Id$
 * @since 5.2M2
 */
@Unstable
public class InstanceInputProperties implements RawProperties
{
    private Map<String, Object> properties;

    private Set<EntityReference> containers;

    public Set<EntityReference> getContainers()
    {
        return this.containers;
    }

    @Override
    public void set(String propertyName, Object value)
    {
        if (this.properties == null) {
            this.properties = new HashMap<String, Object>();
        }

        this.properties.put(propertyName, value);
    }
}
