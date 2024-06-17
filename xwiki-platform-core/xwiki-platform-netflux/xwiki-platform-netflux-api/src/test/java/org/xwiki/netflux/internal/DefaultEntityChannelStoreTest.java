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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.websocket.Session;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link DefaultEntityChannelStore}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultEntityChannelStoreTest
{
    @InjectMockComponents
    private DefaultEntityChannelStore entityChannelStore;

    @MockComponent
    private ChannelStore channelStore;

    @Mock
    private Session session;

    private final WikiReference entityReference = new WikiReference("test");

    @Test
    void createAndGetChannel()
    {
        // The channel is not yet created.
        List<String> path = List.of("one", "two", "three");
        assertFalse(this.entityChannelStore.getChannel(this.entityReference, path).isPresent());

        // Create the channel.
        Channel channel = new Channel("test");
        when(this.channelStore.create()).thenReturn(channel);
        EntityChannel entityChannel = this.entityChannelStore.createChannel(this.entityReference, path);

        assertEquals(this.entityReference, entityChannel.getEntityReference());
        assertEquals(path, entityChannel.getPath());
        assertEquals(channel.getKey(), entityChannel.getKey());
        assertEquals(0, entityChannel.getUserCount());

        // Trying to re-create should result in the same channel.
        when(this.channelStore.get(channel.getKey())).thenReturn(channel);
        assertSame(entityChannel, this.entityChannelStore.createChannel(this.entityReference, path));

        // Add an user to the channel.
        User me = new User(this.session, "mflorea");
        channel.getUsers().put(me.getName(), me);

        // Get should return the existing channel.
        assertSame(entityChannel, this.entityChannelStore.getChannel(this.entityReference, path).get());
        assertSame(entityChannel, this.entityChannelStore.getChannel(channel.getKey()).get());
        assertEquals(1, entityChannel.getUserCount());

        // Disconnect the user and check again the user count.
        me.setConnected(false);
        assertEquals(0, this.entityChannelStore.getChannel(this.entityReference, path).get().getUserCount());

        // Disconnect the raw channel and check the entity channel.
        when(this.channelStore.get(channel.getKey())).thenReturn(null);
        assertFalse(this.entityChannelStore.getChannel(this.entityReference, path).isPresent());
        assertFalse(this.entityChannelStore.getChannel(channel.getKey()).isPresent());
    }

    @Test
    void getChannels()
    {
        Channel channelOne = new Channel("one");
        when(this.channelStore.create()).thenReturn(channelOne);
        EntityChannel entityChannelOne = this.entityChannelStore.createChannel(this.entityReference, List.of("a", "b"));
        when(this.channelStore.get(channelOne.getKey())).thenReturn(channelOne);

        Channel channelTwo = new Channel("two");
        when(this.channelStore.create()).thenReturn(channelTwo);
        EntityChannel entityChannelTwo = this.entityChannelStore.createChannel(this.entityReference, List.of("x"));
        when(this.channelStore.get(channelTwo.getKey())).thenReturn(channelTwo);

        Channel channelThree = new Channel("three");
        when(this.channelStore.create()).thenReturn(channelThree);
        EntityChannel entityChannelThree =
            this.entityChannelStore.createChannel(this.entityReference, List.of("a", "b", "c"));
        when(this.channelStore.get(channelThree.getKey())).thenReturn(channelThree);

        assertEquals(List.of(entityChannelOne, entityChannelTwo, entityChannelThree),
            this.entityChannelStore.getChannels(this.entityReference));

        assertEquals(List.of(entityChannelOne, entityChannelThree),
            this.entityChannelStore.getChannels(this.entityReference, List.of("a", "b")));
    }
}
