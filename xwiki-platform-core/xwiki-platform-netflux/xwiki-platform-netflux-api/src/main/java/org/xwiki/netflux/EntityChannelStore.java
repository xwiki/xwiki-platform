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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * The component used to create and associate channels to XWiki entities.
 * 
 * @version $Id$
 * @since 13.9RC1
 */
@Role
public interface EntityChannelStore
{
    /**
     * @param entityReference an entity reference
     * @return all existing channels associated to the specified entity
     */
    List<EntityChannel> getChannels(EntityReference entityReference);

    /**
     * Looks for the channels associated to the specified entity and having the specified path prefix.
     * 
     * @param entityReference an entity reference
     * @param pathPrefix the path prefix used to match the channels
     * @return all existing channels associated to the specified entity and having the specified path prefix
     */
    default List<EntityChannel> getChannels(EntityReference entityReference, List<String> pathPrefix)
    {
        return getChannels(entityReference).stream().filter(Objects::nonNull)
            .filter(channel -> channel.getPath().size() >= pathPrefix.size()
                && Objects.equals(channel.getPath().subList(0, pathPrefix.size()), pathPrefix))
            .collect(Collectors.toList());
    }

    /**
     * Looks for a channel associated to the specified entity and having the specified path.
     * 
     * @param entityReference the entity the channel is associated with
     * @param path the channel path, used to identify the channel among all the channels associated to the same entity
     * @return the found channel
     */
    default Optional<EntityChannel> getChannel(EntityReference entityReference, List<String> path)
    {
        return getChannels(entityReference).stream().filter(Objects::nonNull)
            .filter(channel -> Objects.equals(channel.getPath(), path)).findFirst();
    }

    /**
     * Retrieve an entity channel by its key.
     *
     * @param key the channel key
     * @return the channel associated with the specified key, if any
     * @since 15.10.11
     * @since 16.4.1
     * @since 16.5.0
     */
    @Unstable
    default Optional<EntityChannel> getChannel(String key)
    {
        return Optional.empty();
    }

    /**
     * Associate a new channel to the specified entity, having the given path.
     * 
     * @param entityReference the entity to associate the channel with
     * @param path the channel path, used to identify the channel among all the channels associated to the same entity
     * @return information about the created channel
     */
    EntityChannel createChannel(EntityReference entityReference, List<String> path);
}
