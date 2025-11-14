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
package org.xwiki.netflux.internal;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.netflux.EntityChannel;

/**
 * The channels.
 * 
 * @version $Id$
 */
@Component(roles = EntityChannels.class)
@Singleton
public class EntityChannels
{
    private final Map<EntityReference, List<EntityChannel>> channels = new ConcurrentHashMap<>();

    /**
     * @param entityReference the reference of the entity
     * @return the channels associated with the entity
     */
    public List<EntityChannel> get(EntityReference entityReference)
    {
        return this.channels.get(entityReference);
    }

    /**
     * @param entityReference the reference of the entity
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}
     */
    public List<EntityChannel> remove(EntityReference entityReference)
    {
        return this.channels.remove(entityReference);
    }

    /**
     * @param entityReference the reference of the entity
     * @param availableChannels the channels associated with the entity
     * @return the previous value associated with {@code key}, or {@code null} if there was no mapping for {@code key}.
     *         (A {@code null} return can also indicate that the map previously associated {@code null} with
     *         {@code key}, if the implementation supports {@code null} values.)
     */
    public List<EntityChannel> put(EntityReference entityReference, List<EntityChannel> availableChannels)
    {
        return this.channels.put(entityReference, availableChannels);
    }
}
