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
package org.xwiki.netflux;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * A channel associated to an XWiki entity.
 * 
 * @version $Id$
 * @since 13.9RC1
 */
public class EntityChannel
{
    private final EntityReference entityReference;

    private final List<String> path;

    private final String key;

    private int userCount;

    /**
     * Creates a new channel.
     * 
     * @param entityReference the entity this channel is associated with
     * @param path the channel path, used to identify the channel among all the channels associated to the same entity
     * @param key the channel key
     */
    public EntityChannel(EntityReference entityReference, List<String> path, String key)
    {
        this.entityReference = entityReference;
        this.path = path;
        this.key = key;
    }

    /**
     * @return the entity associated with this channel
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return the channel path, used to identify the channel among all the channels associated to the same entity
     */
    public List<String> getPath()
    {
        return path;
    }

    /**
     * @return the channel key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @return the number of users connected to the channel
     */
    public int getUserCount()
    {
        return userCount;
    }

    /**
     * Sets the number of users connected to the channel.
     * 
     * @param userCount the new user count
     */
    public void setUserCount(int userCount)
    {
        this.userCount = userCount;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(this.entityReference).append(this.path).append(this.key).toHashCode();
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
        EntityChannel otherChannel = (EntityChannel) object;
        return new EqualsBuilder().append(this.entityReference, otherChannel.entityReference)
            .append(this.path, otherChannel.path).append(this.key, otherChannel.key).isEquals();
    }

    @Override
    public String toString()
    {
        return new XWikiToStringBuilder(this).append("entity", this.entityReference).append("path", this.path)
            .append("key", this.key).build();
    }
}
