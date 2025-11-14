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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.internal.event.EntityChannelCreateEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Create and access {@link EntityChannel}s.
 * 
 * @version $Id$
 * @since 17.10.0
 */
@Component(roles = InternalEntityChannelStore.class)
@Singleton
public class InternalEntityChannelStore
{
    @Inject
    private ChannelStore channelStore;

    @Inject
    private EntityChannels entityChannels;

    @Inject
    private ObservationManager observation;

    /**
     * @param entityReference an entity reference
     * @return all existing channels associated to the specified entity
     */
    public List<EntityChannel> getChannels(EntityReference entityReference)
    {
        List<EntityChannel> channels = this.entityChannels.get(entityReference);
        if (channels != null) {
            this.channelStore.prune();
            List<EntityChannel> availableChannels = channels.stream().filter(this::hasRawChannel)
                .collect(Collectors.toCollection(CopyOnWriteArrayList::new));
            if (availableChannels.isEmpty()) {
                this.entityChannels.remove(entityReference);
            } else {
                this.entityChannels.put(entityReference, availableChannels);
                return Collections.unmodifiableList(availableChannels);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Associate a new channel to the specified entity, having the given path.
     * 
     * @param entityReference the entity to associate the channel with
     * @param path the channel path, used to identify the channel among all the channels associated to the same entity
     * @return information about the created channel
     */
    public synchronized EntityChannel createChannel(EntityReference entityReference, List<String> path)
    {
        Optional<EntityChannel> existingChannel = getChannel(entityReference, path);
        if (existingChannel.isPresent()) {
            // Return existing channel.
            return existingChannel.get();
        }

        // Create a new netflux channel
        String channelKey = this.channelStore.create().getKey();

        // Create the new entity channel
        EntityChannel channel = new EntityChannel(entityReference, path, channelKey);

        // Store the entity channel
        addChannel(channel);

        // Notify about the new channel
        this.observation.notify(new EntityChannelCreateEvent(channelKey, entityReference, path), null);

        return channel;
    }

    /**
     * Associate a new channel to the specified entity, having the given path.
     * 
     * @param channelKey the key of the existing netflux channel
     * @param entityReference the entity to associate the channel with
     * @param path the channel path, used to identify the channel among all the channels associated to the same entity
     * @return information about the created channel
     */
    public synchronized EntityChannel createChannel(String channelKey, EntityReference entityReference, List<String> path)
    {
        EntityChannel channel = new EntityChannel(entityReference, path, channelKey);

        addChannel(channel);

        List<EntityChannel> channels =
            this.entityChannels.computeIfAbsent(entityReference, key -> new CopyOnWriteArrayList<>());
        channels.add(channel);

        TODO: event

        // Ask again the bots to join the channel now that we have an entity channel.
        this.channelStore.askBotsToJoin(this.channelStore.get(channel.getKey()));

        return channel;
    }

    /**
     * Associate a new channel to the specified entity, having the given path.
     * 
     * @param channel the channel to add
     */
    public void addChannel(EntityChannel channel)
    {
        List<EntityChannel> channels =
            this.entityChannels.computeIfAbsent(entityReference, key -> new CopyOnWriteArrayList<>());
        channels.add(channel);

        TODO: event

        // Ask again the bots to join the channel now that we have an entity channel.
        this.channelStore.askBotsToJoin(this.channelStore.get(channel.getKey()));

        return channel;
    }

    /**
     * Retrieve an entity channel by its key.
     *
     * @param key the channel key
     * @return the channel associated with the specified key, if any
     */
    public Optional<EntityChannel> getChannel(String key)
    {
        return this.entityChannels.values().stream().flatMap(List::stream)
            .filter(channel -> Objects.equals(channel.getKey(), key)).filter(this::hasRawChannel).findFirst();
    }

    private boolean hasRawChannel(EntityChannel channel)
    {
        Channel rawChannel = this.channelStore.get(channel.getKey());
        if (rawChannel != null) {
            channel.setUserCount(rawChannel.getUsers().size());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Looks for a channel associated to the specified entity and having the specified path.
     * 
     * @param entityReference the entity the channel is associated with
     * @param path the channel path, used to identify the channel among all the channels associated to the same entity
     * @return the found channel
     */
    Optional<EntityChannel> getChannel(EntityReference entityReference, List<String> path)
    {
        return getChannels(entityReference).stream().filter(Objects::nonNull)
            .filter(channel -> Objects.equals(channel.getPath(), path)).findFirst();
    }
}
