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
package org.xwiki.observation.remote.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.observation.event.ApplicationStartedEvent;
import org.xwiki.observation.event.ApplicationStoppedEvent;
import org.xwiki.observation.remote.ClusterMemberDepartureManager;
import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.observation.remote.NetworkMemberJoinedEvent;
import org.xwiki.observation.remote.NetworkMemberLeftEvent;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ClusterMemberDepartureListener}.
 *
 * @version $Id$
 */
@ComponentTest
class ClusterMemberDepartureListenerTest
{
    @InjectMockComponents
    private ClusterMemberDepartureListener listener;

    @MockComponent
    private RemoteObservationManagerConfiguration configuration;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private DefaultClusterMemberDepartureManager manager;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        this.manager = mock(DefaultClusterMemberDepartureManager.class);
        this.componentManager.registerComponent(ClusterMemberDepartureManager.class, this.manager);
    }

    @Test
    void getEvents()
    {
        assertEquals(4, this.listener.getEvents().size());
    }

    @Test
    void onNetworkEvents()
    {
        when(this.configuration.isEnabled()).thenReturn(true);

        NetworkMember member = mock(NetworkMember.class);
        when(member.getId()).thenReturn("member");
        NetworkChannel channel = mock(NetworkChannel.class);

        this.listener.onEvent(new NetworkMemberLeftEvent("tcp"), channel, member);
        verify(this.manager).updateAsync("member");

        this.listener.onEvent(new NetworkMemberLeftEvent("tcp"), channel, null);
        this.listener.onEvent(new NetworkMemberJoinedEvent("tcp"), channel, member);
        this.listener.onEvent(new ApplicationStartedEvent(), null, null);
        verify(this.manager, times(3)).updateAsync(null);
    }

    @Test
    void onEventWhenDisabled()
    {
        this.listener.onEvent(new ApplicationStartedEvent(), null, null);

        verifyNoInteractions(this.manager);
    }

    @Test
    void onApplicationStopped()
    {
        this.listener.onEvent(new ApplicationStoppedEvent(), null, null);

        verify(this.manager).stop();
    }

    @Test
    void onEventWithOtherImplementation()
    {
        ClusterMemberDepartureManager otherManager = mock(ClusterMemberDepartureManager.class);
        ReflectionUtils.setFieldValue(this.listener, "departureManager", otherManager);
        when(this.configuration.isEnabled()).thenReturn(true);

        this.listener.onEvent(new ApplicationStoppedEvent(), null, null);
        this.listener.onEvent(new NetworkMemberLeftEvent("tcp"), null, null);

        verifyNoInteractions(otherManager);
    }
}
