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

import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * A channel associated to an XWiki entity.
 * 
 * @version $Id$
 * @since 13.7RC1
 */
@Unstable
public class EntityChannel
{
    private final EntityReference entityReference;

    private final String key;

    private final String type;

    private int userCount;

    /**
     * Creates a new channel.
     * 
     * @param entityReference the entity this channel is associated with
     * @param key the channel key
     * @param type the channel type
     */
    public EntityChannel(EntityReference entityReference, String key, String type)
    {
        this.entityReference = entityReference;
        this.key = key;
        this.type = type;
    }

    /**
     * @return the entity associated with this channel
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return the channel key
     */
    public String getKey()
    {
        return key;
    }

    /**
     * @return the channel type
     */
    public String getType()
    {
        return type;
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
}
