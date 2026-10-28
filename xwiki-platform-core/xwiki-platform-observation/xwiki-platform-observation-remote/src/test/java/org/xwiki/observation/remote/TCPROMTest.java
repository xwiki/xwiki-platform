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
package org.xwiki.observation.remote;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.jgroups.Address;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannel;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkMember;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.observation.remote.test.TestEvent;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test checking that event are well converted and transported from one cluster member to another.
 * 
 * @version $Id$
 */
@AllComponents
class TCPROMTest extends AbstractROMTestCase
{
    static class Unserializable
    {
    }

    private RemoteObservationManager rom1;

    private RemoteObservationManager rom2;

    private final Map<String, String> previousProperties = new HashMap<>();

    @Override
    @BeforeEach
    public void beforeEach() throws Exception
    {
        super.beforeEach();

        setProperty("jgroups.bind_addr", "localhost");
        // tcp.xml only probes localhost[7800] plus jgroups.tcp.port_range ports, and we start up to three members
        setProperty("jgroups.tcp.port_range", "5");

        getConfigurationSource1().setProperty("observation.remote.channels", Arrays.asList("tcp"));
        this.rom1 = getComponentManager1().getInstance(RemoteObservationManager.class);
        getConfigurationSource2().setProperty("observation.remote.channels", Arrays.asList("tcp"));
        this.rom2 = getComponentManager2().getInstance(RemoteObservationManager.class);
    }

    @AfterEach
    void restoreProperties()
    {
        for (Map.Entry<String, String> entry : this.previousProperties.entrySet()) {
            if (entry.getValue() == null) {
                System.clearProperty(entry.getKey());
            } else {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    private void setProperty(String key, String value)
    {
        this.previousProperties.put(key, System.setProperty(key, value));
    }

    /**
     * Validate sharing a simple Serializable event between two instances of {@link RemoteObservationManager}.
     */
    @Test
    void serializableEvent() throws InterruptedException
    {
        EventListener localListener = mock(EventListener.class, "local");
        EventListener remoteListener = mock(EventListener.class, "remote");

        TestEvent event = new TestEvent();

        Unserializable unserializable = new Unserializable();

        when(localListener.getName()).thenReturn("mylistener");
        when(remoteListener.getName()).thenReturn("mylistener");
        when(localListener.getEvents()).thenReturn(Arrays.asList(event));
        when(remoteListener.getEvents()).thenReturn(Arrays.asList(event));

        getObservationManager1().addListener(localListener);
        getObservationManager2().addListener(remoteListener);

        // By default, fully serializable events and parameters
        getObservationManager1().notify(event, "some source", "some data");
        // By default, unserializable parameters are not sent
        getObservationManager1().notify(event, unserializable, unserializable);
        // A custom converter prevent from sending log events
        getObservationManager1().notify(new LogEvent(), "some source", "some data");

        // Make sure JGroups has enough time to send the message
        Thread.sleep(1000);

        verify(localListener).onEvent(same(event), eq("some source"), eq("some data"));
        verify(localListener).onEvent(same(event), same(unserializable), same(unserializable));
        verify(remoteListener).onEvent(event, "some source", "some data");
    }

    @Test
    void members()
    {
        NetworkChannel channel1 = this.rom1.getChannels().iterator().next();
        NetworkChannel channel2 = this.rom2.getChannels().iterator().next();

        assertEquals(List.copyOf(channel1.getMembers()), List.copyOf(channel2.getMembers()));
        assertEquals(channel1.getLeader(), channel2.getLeader());
    }

    /**
     * Validate that the remaining members are notified about the member which actually left, and forget it.
     */
    @Test
    void memberLeft() throws Exception
    {
        NetworkChannel channel1 = this.rom1.getChannels().iterator().next();
        NetworkChannel channel2 = this.rom2.getChannels().iterator().next();
        // The first instance learns about the second one asynchronously
        waitUntil(() -> channel1.getMembers().size() == 2, "The second instance did not join the cluster");

        EventListener listener1 = mockListener("listener1", new NetworkMemberLeftEvent());
        EventListener listener2 = mockListener("listener2", new NetworkMemberLeftEvent());
        getObservationManager1().addListener(listener1);
        getObservationManager2().addListener(listener2);

        // Start a third instance and wait for the two other instances to see it
        MockitoComponentManager componentManager3 = new MockitoComponentManager();
        String id3;
        try {
            MemoryConfigurationSource configurationSource3 =
                initializeComponentManager(componentManager3, mock(Environment.class), "instance3");
            configurationSource3.setProperty("observation.remote.channels", Arrays.asList("tcp"));
            componentManager3.getInstance(RemoteObservationManager.class);
            id3 = componentManager3.<RemoteObservationManagerConfiguration>getInstance(
                RemoteObservationManagerConfiguration.class).getId();

            waitUntil(() -> channel1.getMembers().size() == 3 && channel2.getMembers().size() == 3,
                "The third instance did not join the cluster");
        } finally {
            // Stop the third instance
            disposeComponentManager(componentManager3);
        }

        // The two other instances should notice that the third one left
        waitUntil(() -> channel1.getMembers().size() == 2 && channel2.getMembers().size() == 2,
            "The third instance was not removed from the members");
        assertTrue(channel1.getMembers().stream().noneMatch(member -> id3.equals(member.getId())));
        assertEquals(List.copyOf(channel1.getMembers()), List.copyOf(channel2.getMembers()));
        assertEquals(channel1.getLeader(), channel2.getLeader());

        // The member indicated in the event should be the one which actually left
        assertMemberLeft(listener1, channel1, id3);
        assertMemberLeft(listener2, channel2, id3);
    }

    /**
     * Validate that a member whose id was forgotten (for example because it was wrongly suspected, its id being
     * forgotten with the view which excluded it) is asked again for its id when it's part of a new view, and shows up
     * again in the members.
     */
    @Test
    @SuppressWarnings("unchecked")
    void memberIdForgotten() throws Exception
    {
        JGroupsNetworkChannel channel1 = (JGroupsNetworkChannel) this.rom1.getChannels().iterator().next();
        NetworkChannel channel2 = this.rom2.getChannels().iterator().next();
        waitUntil(() -> channel1.getMembers().size() == 2, "The second instance did not join the cluster");
        String id2 = componentManager2.<RemoteObservationManagerConfiguration>getInstance(
            RemoteObservationManagerConfiguration.class).getId();
        JGroupsNetworkMember member2 = (JGroupsNetworkMember) channel1.getMembers().stream()
            .filter(member -> id2.equals(member.getId())).findFirst().get();

        EventListener leftListener = mockListener("left", new NetworkMemberLeftEvent());
        EventListener joinedListener = mockListener("joined", new NetworkMemberJoinedEvent());
        getObservationManager1().addListener(leftListener);
        getObservationManager1().addListener(joinedListener);

        // Simulate the second instance being wrongly suspected: its id is forgotten and a view (which in the real
        // scenario is the merge view) containing it again is received
        Map<Address, String> membersIdMap = (Map<Address, String>) FieldUtils.readField(channel1, "membersIdMap", true);
        membersIdMap.remove(member2.getAddress());
        channel1.viewAccepted(channel1.getJChannel().getView());

        // The member disappears since its id is not known anymore...
        assertMember(leftListener, NetworkMemberLeftEvent.class, channel1, id2);
        // ... and comes back as soon as its id is received again
        assertMember(joinedListener, NetworkMemberJoinedEvent.class, channel1, id2);
        waitUntil(() -> channel1.getMembers().size() == 2, "The second instance did not come back in the members");
        assertEquals(List.copyOf(channel1.getMembers()), List.copyOf(channel2.getMembers()));
        assertEquals(id2, membersIdMap.get(member2.getAddress()));
    }

    /**
     * Validate that the leader is the oldest member of the view (the JGroups coordinator) even when its id is not
     * known, instead of the oldest member whose id is known (which would be the current instance here).
     */
    @Test
    @SuppressWarnings("unchecked")
    void leaderIdUnknown() throws Exception
    {
        NetworkChannel channel1 = this.rom1.getChannels().iterator().next();
        JGroupsNetworkChannel channel2 = (JGroupsNetworkChannel) this.rom2.getChannels().iterator().next();
        waitUntil(() -> channel1.getMembers().size() == 2, "The second instance did not join the cluster");
        String id1 = componentManager1.<RemoteObservationManagerConfiguration>getInstance(
            RemoteObservationManagerConfiguration.class).getId();
        Address coordinator = channel2.getJChannel().getView().getCoord();
        // The first instance is the oldest member of the cluster
        assertEquals(id1, channel2.getLeader().getId());
        assertEquals(coordinator, ((JGroupsNetworkMember) channel2.getLeader()).getAddress());

        EventListener leaderListener = mockListener("leader", new NetworkChannelLeaderChangedEvent());
        getObservationManager2().addListener(leaderListener);

        // Simulate the id of the coordinator not being known by the second instance (forgotten as in memberIdForgotten
        // or not received yet)
        Map<Address, String> membersIdMap = (Map<Address, String>) FieldUtils.readField(channel2, "membersIdMap", true);
        membersIdMap.remove(coordinator);
        channel2.viewAccepted(channel2.getJChannel().getView());

        // The leader stays the coordinator (with an unknown id) and becomes complete when the id is received again
        ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
        verify(leaderListener, timeout(10000).times(2)).onEvent(any(NetworkChannelLeaderChangedEvent.class),
            same(channel2), data.capture());
        JGroupsNetworkMember unknownLeader = (JGroupsNetworkMember) data.getAllValues().get(0);
        assertEquals(coordinator, unknownLeader.getAddress());
        assertNull(unknownLeader.getId());
        JGroupsNetworkMember knownLeader = (JGroupsNetworkMember) data.getAllValues().get(1);
        assertEquals(coordinator, knownLeader.getAddress());
        assertEquals(id1, knownLeader.getId());
        assertEquals(channel1.getLeader(), channel2.getLeader());
    }

    private EventListener mockListener(String name, Event event)
    {
        EventListener listener = mock(EventListener.class, name);
        when(listener.getName()).thenReturn(name);
        when(listener.getEvents()).thenReturn(List.of(event));

        return listener;
    }

    private void assertMemberLeft(EventListener listener, NetworkChannel channel, String expectedId)
    {
        assertMember(listener, NetworkMemberLeftEvent.class, channel, expectedId);
    }

    private void assertMember(EventListener listener, Class<? extends Event> eventClass, NetworkChannel channel,
        String expectedId)
    {
        ArgumentCaptor<Object> data = ArgumentCaptor.forClass(Object.class);
        verify(listener, timeout(10000)).onEvent(any(eventClass), same(channel), data.capture());
        assertEquals(expectedId, ((NetworkMember) data.getValue()).getId());
    }

    private void waitUntil(BooleanSupplier condition, String message) throws InterruptedException
    {
        long end = System.currentTimeMillis() + 30000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > end) {
                fail(message);
            }
            Thread.sleep(50);
        }
    }
}
