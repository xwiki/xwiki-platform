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
package org.xwiki.wiki.properties;

import java.util.HashMap;
import java.util.Map;

/**
 * Property group is a place where modules can store their own properties concerning a wiki. This group is then attached
 * to the WikiDescriptor. Each module is responsible for the persistent storage of its properties.
 *
 * @version $Id$
 * @since 5.3M2
 */
public class WikiPropertyGroup implements Cloneable
{
    /**
     * Unique identifier of the property group.
     */
    private String id;

    /**
     * Properties.
     */
    private Map<String, Object> properties;

    /**
     * Constructor.
     *
     * @param id Unique identifier of the group
     */
    public WikiPropertyGroup(String id)
    {
        this.id = id;
        this.properties = new HashMap<String, Object>();
    }

    /**
     * @return the unique identifier of the group
     */
    public String getId()
    {
        return id;
    }

    /**
     * Get the value of a property.
     *
     * @param propertyId Id of the property
     * @return the value of the property
     */
    public Object get(String propertyId)
    {
        return properties.get(propertyId);
    }

    /**
     * Set the value of a property.
     *
     * @param propertyId Id of the property to change
     * @param value value so store in the property
     */
    public void set(String propertyId, Object value)
    {
        properties.put(propertyId, value);
    }

    @Override
    public WikiPropertyGroup clone()
    {
        WikiPropertyGroup group;
        try {
            group = (WikiPropertyGroup) super.clone();
        } catch (CloneNotSupportedException e) {
            // Supposed to be impossible
            return new WikiPropertyGroup(getId());
        }

        // Clone the map
        group.properties = new HashMap<>(this.properties);

        return group;
    }
}
