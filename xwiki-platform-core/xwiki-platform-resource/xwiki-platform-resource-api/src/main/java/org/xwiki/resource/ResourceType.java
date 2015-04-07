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
package org.xwiki.resource;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.stability.Unstable;

/**
 * Represents the type of Resource (eg Entity, Temporary, etc).
 *
 * @version $Id$
 * @since 6.1M2
 */
@Unstable
public class ResourceType
{
    /**
     * The resource type id (e.g. "bin", "tmp", etc).
     */
    private String id;

    /**
     * @param id see {@link #getId()}
     */
    public ResourceType(String id)
    {
        this.id = id;
    }

    /**
     * Converts a string to an {@link ResourceType} instance.
     *
     * @param id see {@link #getId()}
     * @return the object representing the Resource Type id passed as a string
     */
    public static ResourceType fromString(String id)
    {
        return new ResourceType(id);
    }

    /**
     * @return the technical id of the Resource Type
     */
    public String getId()
    {
        return this.id;
    }

    @Override
    public String toString()
    {
        return getId();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(13, 7)
            .append(getId())
            .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        ResourceType rhs = (ResourceType) object;
        return new EqualsBuilder()
            .append(getId(), rhs.getId())
            .isEquals();
    }
}
