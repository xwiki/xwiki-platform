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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.ClusterSingletonNodeChangedEvent;
import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.observation.remote.RemoteObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannel;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkMember;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultClusterSingletonManager}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultClusterSingletonManagerTest
{
    private static final String CHANNEL = "tcp";

    private static final long TIMEOUT = 10000;

    @InjectMockComponents
    private DefaultClusterSingletonManager manager;

    @MockComponent
    private RemoteObservationManagerConfiguration configuration;

    @MockComponent
    private RemoteObservationManager remoteObservationManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private ExecutionContextManager executionContextManager;

    @MockComponent
    private Execution execution;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private NetworkChannel channel;

    @BeforeEach
    void beforeEach()
    {
        when(this.configuration.isEnabled()).thenReturn(true);
        when(this.configuration.getChannels()).thenReturn(List.of(CHANNEL));

        this.channel = mockChannel(CHANNEL);
        when(this.remoteObservationManager.getChannels()).thenReturn(List.of(this.channel));
    }

    private NetworkChannel mockChannel(String id, NetworkMember... members)
    {
        NetworkChannel networkChannel = mock(NetworkChannel.class);
        when(networkChannel.getId()).thenReturn(id);
        when(networkChannel.getMembers()).thenReturn(List.of(members));

        return networkChannel;
    }

    private NetworkMember mockMember(String id, int priority, boolean local)
    {
        NetworkMember member = mock(NetworkMember.class);
        when(member.getId()).thenReturn(id);
        when(member.getSingletonPriority()).thenReturn(priority);
        when(member.isLocal()).thenReturn(local);

        return member;
    }

    private void setMembers(NetworkMember... members)
    {
        when(this.channel.getMembers()).thenReturn(List.of(members));
    }

    private boolean update()
    {
        this.manager.update();

        return this.manager.isSingletonNode();
    }

    private void verifyNotified(boolean singletonNode)
    {
        verify(this.observationManager).notify(any(ClusterSingletonNodeChangedEvent.class), same(this.manager),
            eq(singletonNode));
    }

    @Test
    void isSingletonNodeWhenDisabled()
    {
        when(this.configuration.isEnabled()).thenReturn(false);

        assertTrue(this.manager.isSingletonNode());
        verifyNoInteractions(this.remoteObservationManager);

        // The loss is notified when stopping, even without cluster
        this.manager.stop();
        this.manager.stop();

        assertFalse(this.manager.isSingletonNode());
        verifyNotified(false);
        verify(this.observationManager).notify(any(), any(), any());
        verifyNoInteractions(this.remoteObservationManager);
    }

    @Test
    void isSingletonNodeWithoutRunningChannel()
    {
        // The channel is not started yet: the first call computes the answer without logging it
        assertTrue(this.manager.isSingletonNode());
        assertEquals(0, this.logCapture.size());

        // Another channel is running but it's not configured
        NetworkChannel otherChannel = mockChannel("other", mockMember("local", 0, true));
        when(this.remoteObservationManager.getChannels()).thenReturn(List.of(otherChannel));

        assertTrue(update());
        assertTrue(update());

        assertEquals("Remote observation is enabled but none of the configured channels [tcp] is running: this"
            + " instance runs the tasks executed only once in the cluster.", this.logCapture.getMessage(0));
        assertEquals("This instance is the cluster singleton node (no running channel)",
            this.logCapture.getMessage(1));
        // The warning and the state are logged only once
        assertEquals(2, this.logCapture.size());
    }

    @Test
    void isSingletonNodeWithNegativePriority()
    {
        when(this.configuration.getSingletonPriority()).thenReturn(-1);

        // A negative priority wins over the absence of running channel
        assertFalse(this.manager.isSingletonNode());
        assertFalse(update());

        assertEquals("This instance is not the cluster singleton node (negative singleton priority)",
            this.logCapture.getMessage(0));
        verifyNoInteractions(this.remoteObservationManager);
    }

    @Test
    void isSingletonNodeWhenRemoteObservationManagerFails()
    {
        when(this.remoteObservationManager.getChannels()).thenThrow(new RuntimeException("error"));

        assertTrue(this.manager.isSingletonNode());
    }

    @Test
    void isSingletonNodeIsCached()
    {
        setMembers(mockMember("local", 0, true));
        assertTrue(this.manager.isSingletonNode());

        // The answer changes only when updated
        setMembers(mockMember("remote", 0, false), mockMember("local", 0, true));
        assertTrue(this.manager.isSingletonNode());
        assertFalse(update());

        this.logCapture.ignoreAllMessages();
    }

    @Test
    void isSingletonNodeElection()
    {
        NetworkMember local0 = mockMember("local", 0, true);
        NetworkMember remote0 = mockMember("remote", 0, false);

        // Same priority: the oldest member wins
        setMembers(local0, remote0);
        assertTrue(update());
        setMembers(remote0, local0);
        assertFalse(update());

        // The highest priority wins
        setMembers(remote0, mockMember("local", 10, true));
        assertTrue(update());
        setMembers(local0, mockMember("remote", 10, false));
        assertFalse(update());
        setMembers(mockMember("remote", -2, false), local0);
        assertTrue(update());

        // A negative priority is never elected
        setMembers(mockMember("local", 0, true), mockMember("remote", -2, false));
        when(this.configuration.getSingletonPriority()).thenReturn(-1);
        assertFalse(update());

        assertEquals("This instance is the cluster singleton node (channel [tcp], members [local (priority 0, this"
            + " instance), remote (priority 0)])", this.logCapture.getMessage(0));
        assertEquals("This instance is no longer the cluster singleton node (channel [tcp], members [remote (priority"
            + " 0), local (priority 0, this instance)])", this.logCapture.getMessage(1));
        assertEquals("This instance is now the cluster singleton node (channel [tcp], members [remote (priority 0),"
            + " local (priority 10, this instance)])", this.logCapture.getMessage(2));
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void isSingletonNodeWithSeveralChannels()
    {
        when(this.configuration.getChannels()).thenReturn(List.of("first", CHANNEL));
        // The first configured channel is not running (the current instance is not one of its members)
        NetworkChannel firstChannel = mockChannel("first", mockMember("remote", 0, false));
        when(this.remoteObservationManager.getChannels()).thenReturn(List.of(this.channel, firstChannel));

        setMembers(mockMember("local", 0, true), mockMember("remote", 0, false));

        assertTrue(update());

        assertEquals("This instance is the cluster singleton node (channel [tcp], members [local (priority 0, this"
            + " instance), remote (priority 0)])", this.logCapture.getMessage(0));
    }

    @Test
    void isSingletonNodeFromJGroupsView()
    {
        // The election uses the members of the JGroups view, including the ones whose identifier is not known yet
        JGroupsNetworkChannel jgroupsChannel = mock(JGroupsNetworkChannel.class);
        when(jgroupsChannel.getId()).thenReturn(CHANNEL);
        JGroupsNetworkMember worker = mock(JGroupsNetworkMember.class);
        when(worker.getSingletonPriority()).thenReturn(10);
        JGroupsNetworkMember local = mock(JGroupsNetworkMember.class);
        when(local.getId()).thenReturn("local");
        when(local.isLocal()).thenReturn(true);
        when(jgroupsChannel.getViewMembers()).thenReturn(List.of(local, worker));
        when(this.remoteObservationManager.getChannels()).thenReturn(List.of(jgroupsChannel));

        assertFalse(update());

        assertEquals("This instance is not the cluster singleton node (channel [tcp], members [local (priority 0,"
            + " this instance), null (priority 10)])", this.logCapture.getMessage(0));
        verify(jgroupsChannel, never()).getMembers();
    }

    @Test
    void changesAreNotified()
    {
        NetworkMember local = mockMember("local", 0, true);
        NetworkMember worker = mockMember("worker", 10, false);

        setMembers(local);
        assertTrue(update());

        // Same answer: nothing is notified
        assertTrue(update());
        verify(this.observationManager, never()).notify(any(), any(), any());

        // A member with a higher priority joins
        setMembers(local, worker);
        assertFalse(update());
        verifyNotified(false);

        assertEquals("This instance is the cluster singleton node (channel [tcp], members [local (priority 0, this"
            + " instance)])", this.logCapture.getMessage(0));
        assertEquals("This instance is no longer the cluster singleton node (channel [tcp], members [local (priority"
            + " 0, this instance), worker (priority 10)])", this.logCapture.getMessage(1));
    }

    @Test
    void updateAsync()
    {
        AtomicReference<String> notificationThread = new AtomicReference<>();
        doAnswer(invocation -> {
            notificationThread.set(Thread.currentThread().getName());
            return null;
        }).when(this.observationManager).notify(any(), any(), any());

        setMembers(mockMember("local", 0, true));
        assertTrue(update());

        // The member with a higher priority joins, the update being triggered by a network event
        setMembers(mockMember("local", 0, true), mockMember("worker", 10, false));
        this.manager.updateAsync();
        verify(this.observationManager, timeout(TIMEOUT)).notify(any(ClusterSingletonNodeChangedEvent.class),
            same(this.manager), eq(false));

        assertEquals("XWiki cluster singleton node", notificationThread.get());
        assertFalse(this.manager.isSingletonNode());

        this.logCapture.ignoreAllMessages();
    }

    @Test
    void stop()
    {
        setMembers(mockMember("local", 0, true));
        assertTrue(update());

        this.manager.stop();

        // Notified synchronously
        verifyNotified(false);
        assertFalse(this.manager.isSingletonNode());

        // Nothing changes anymore
        this.manager.update();
        verify(this.observationManager, times(1)).notify(any(), any(), any());

        assertEquals("This instance is no longer the cluster singleton node (the instance is stopping)",
            this.logCapture.getMessage(1));
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void stopWhenNotSingletonNode()
    {
        setMembers(mockMember("remote", 0, false), mockMember("local", 0, true));
        assertFalse(update());

        this.manager.stop();

        verify(this.observationManager, never()).notify(any(), any(), any());
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void updateAfterDispose()
    {
        setMembers(mockMember("local", 0, true));
        assertTrue(update());

        this.manager.dispose();

        // The updates requested after the dispose are ignored
        setMembers(mockMember("remote", 0, false), mockMember("local", 0, true));
        this.manager.updateAsync();

        assertTrue(this.manager.isSingletonNode());
        verify(this.observationManager, never()).notify(any(), any(), any());

        this.logCapture.ignoreAllMessages();
    }
}
