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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;

/**
 * Default {@link EntityChannelStore} implementation.
 * 
 * @version $Id$
 * @since 13.9RC1
 */
@Component
@Singleton
public class DefaultEntityChannelStore implements EntityChannelStore
{
    @Inject
    private ChannelStore channelStore;

    private final Map<EntityReference, List<EntityChannel>> entityChannels = new ConcurrentHashMap<>();

    @Override
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

    @Override
    public synchronized EntityChannel createChannel(EntityReference entityReference, List<String> path)
    {
        Optional<EntityChannel> existingChannel = getChannel(entityReference, path);
        if (existingChannel.isPresent()) {
            // Return existing channel.
            return existingChannel.get();
        }

        // Create new channel.
        EntityChannel channel = new EntityChannel(entityReference, path, this.channelStore.create().getKey());
        List<EntityChannel> channels =
            this.entityChannels.computeIfAbsent(entityReference, key -> new CopyOnWriteArrayList<>());
        channels.add(channel);

        // Ask again the bots to join the channel now that we have an entity channel.
        this.channelStore.askBotsToJoin(this.channelStore.get(channel.getKey()));

        return channel;
    }

    @Override
    public Optional<EntityChannel> getChannel(String key)
    {
        return this.entityChannels.values().stream().flatMap(List::stream)
            .filter(channel -> Objects.equals(channel.getKey(), key)).filter(this::hasRawChannel).findFirst();
    }

    private boolean hasRawChannel(EntityChannel channel)
    {
        Channel rawChannel = this.channelStore.get(channel.getKey());
        if (rawChannel != null) {
            channel.setUserCount(rawChannel.getConnectedUsers().size());
            return true;
        } else {
            return false;
        }
    }
}
