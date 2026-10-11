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
package com.xpn.xwiki.plugin.scheduler.internal;

import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.remote.ClusterSingletonManager;
import org.xwiki.observation.remote.ClusterSingletonNodeChangedEvent;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.plugin.scheduler.SchedulerPlugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SchedulerClusterSingletonListener}.
 *
 * @version $Id$
 */
@ComponentTest
class SchedulerClusterSingletonListenerTest
{
    @InjectMockComponents
    private SchedulerClusterSingletonListener listener;

    @MockComponent
    private RemoteObservationManagerConfiguration remoteObservationConfiguration;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @MockComponent
    private ClusterSingletonManager singletonManager;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    private SchedulerPlugin plugin;

    @BeforeEach
    void beforeEach()
    {
        this.plugin = mock(SchedulerPlugin.class);
        this.listener.setSchedulerPlugin(this.plugin);
        when(this.plugin.isEnabled()).thenReturn(true);
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(true);
        when(this.configuration.getProperty(SchedulerClusterSingletonListener.PROPERTY_CLUSTER_SINGLETON, false))
            .thenReturn(true);
    }

    private void notifySingletonNodeChanged(boolean singletonNode)
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(singletonNode);
        this.listener.onEvent(new ClusterSingletonNodeChangedEvent(), this.singletonManager, singletonNode);
    }

    @Test
    void events()
    {
        assertEquals(SchedulerClusterSingletonListener.NAME, this.listener.getName());
        assertTrue(this.listener.getEvents().stream().anyMatch(event -> event.matches(new ApplicationReadyEvent())));
        assertTrue(this.listener.getEvents().stream()
            .anyMatch(event -> event.matches(new ClusterSingletonNodeChangedEvent())));
    }

    @Test
    void isSingletonNodeRequired()
    {
        assertTrue(this.listener.isSingletonNodeRequired());

        // Disabled by default
        when(this.configuration.getProperty(SchedulerClusterSingletonListener.PROPERTY_CLUSTER_SINGLETON, false))
            .thenReturn(false);

        assertFalse(this.listener.isSingletonNodeRequired());

        when(this.configuration.getProperty(SchedulerClusterSingletonListener.PROPERTY_CLUSTER_SINGLETON, false))
            .thenReturn(true);
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(false);

        assertFalse(this.listener.isSingletonNodeRequired());
    }

    @Test
    void startOnTheSingletonNodeThenLose()
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        this.listener.onEvent(new ApplicationReadyEvent(), null, null);

        verify(this.plugin).startScheduler();
        assertEquals("Starting the scheduler: this instance is the cluster singleton node",
            this.logCapture.getMessage(0));

        notifySingletonNodeChanged(false);

        verify(this.plugin).stopScheduler();
        assertEquals("Stopping the scheduler: this instance is no longer the cluster singleton node",
            this.logCapture.getMessage(1));
    }

    @Test
    void startOnAnotherMemberThenGain()
    {
        when(this.singletonManager.isSingletonNode()).thenReturn(false);

        this.listener.onEvent(new ApplicationReadyEvent(), null, null);

        verify(this.plugin, never()).startScheduler();

        notifySingletonNodeChanged(true);

        verify(this.plugin).startScheduler();
        verify(this.plugin, never()).stopScheduler();
        assertEquals("Starting the scheduler: this instance became the cluster singleton node",
            this.logCapture.getMessage(0));
    }

    @Test
    void changeBeforeTheApplicationIsReady()
    {
        // The application ready event decides
        notifySingletonNodeChanged(true);

        verifyNoInteractions(this.plugin);
    }

    @Test
    void withoutCluster()
    {
        when(this.remoteObservationConfiguration.isEnabled()).thenReturn(false);
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        // The plugin starts the scheduler itself when it's initialized
        this.listener.onEvent(new ApplicationReadyEvent(), null, null);
        notifySingletonNodeChanged(false);

        verify(this.plugin, never()).startScheduler();
        verify(this.plugin, never()).stopScheduler();
    }

    @Test
    void singletonNodeWithDisabledScheduler()
    {
        when(this.plugin.isEnabled()).thenReturn(false);
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        this.listener.onEvent(new ApplicationReadyEvent(), null, null);

        verify(this.plugin, never()).startScheduler();
        assertEquals("This instance is the cluster singleton node but its scheduler is disabled"
            + " (scheduler.enabled=false): no cluster member runs the scheduler jobs. Enable the scheduler on this"
            + " instance, or give a higher observation.remote.singletonPriority to a member where it's enabled.",
            this.logCapture.getMessage(0));

        notifySingletonNodeChanged(false);

        verify(this.plugin, never()).stopScheduler();
    }

    @Test
    void gainWithDisabledScheduler()
    {
        when(this.plugin.isEnabled()).thenReturn(false);
        when(this.singletonManager.isSingletonNode()).thenReturn(false);
        this.listener.onEvent(new ApplicationReadyEvent(), null, null);

        notifySingletonNodeChanged(true);

        verify(this.plugin, never()).startScheduler();
        assertTrue(this.logCapture.getMessage(0).startsWith("This instance is the cluster singleton node but its"));
    }

    @Test
    void withoutPlugin()
    {
        this.listener.setSchedulerPlugin(null);
        when(this.singletonManager.isSingletonNode()).thenReturn(true);

        this.listener.onEvent(new ApplicationReadyEvent(), null, null);
        notifySingletonNodeChanged(false);

        verifyNoInteractions(this.plugin);
    }
}
