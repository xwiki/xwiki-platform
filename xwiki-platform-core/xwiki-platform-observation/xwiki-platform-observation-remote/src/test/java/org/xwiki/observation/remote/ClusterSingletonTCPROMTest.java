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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.test.annotation.AllComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Integration test of the election of the cluster singleton node between two instances connected with the JGroups TCP
 * stack, the singleton priority being carried by the JGroups addresses.
 *
 * @version $Id$
 */
@AllComponents
class ClusterSingletonTCPROMTest extends AbstractROMTestCase
{
    private static final String CHANNEL = "tcp";

    private static final long TIMEOUT = 20000;

    private String previousBindAddress;

    private RemoteObservationManager rom1;

    private RemoteObservationManager rom2;

    private ClusterSingletonManager singletonManager1;

    private ClusterSingletonManager singletonManager2;

    @Override
    @BeforeEach
    public void beforeEach() throws Exception
    {
        super.beforeEach();

        this.previousBindAddress = System.setProperty("jgroups.bind_addr", "localhost");

        getConfigurationSource1().setProperty("observation.remote.channels", List.of(CHANNEL));
        getConfigurationSource2().setProperty("observation.remote.channels", List.of(CHANNEL));

        // Make sure each instance has its own identifier (the members are indexed by identifier)
        setId(getComponentManager1(), getConfigurationSource1(), "instance1");
        setId(getComponentManager2(), getConfigurationSource2(), "instance2");

        this.singletonManager1 = getComponentManager1().getInstance(ClusterSingletonManager.class);
        this.singletonManager2 = getComponentManager2().getInstance(ClusterSingletonManager.class);
    }

    private void setId(ComponentManager componentManager, MemoryConfigurationSource configurationSource, String id)
        throws Exception
    {
        configurationSource.setProperty("observation.remote.id", id);
        ((Initializable) componentManager.getInstance(RemoteObservationManagerConfiguration.class)).initialize();
    }

    @AfterEach
    void restoreBindAddress()
    {
        if (this.previousBindAddress != null) {
            System.setProperty("jgroups.bind_addr", this.previousBindAddress);
        } else {
            System.clearProperty("jgroups.bind_addr");
        }
    }

    private void startInstances() throws Exception
    {
        this.rom1 = getComponentManager1().getInstance(RemoteObservationManager.class);
        startInstance2();
    }

    private void startInstance2() throws Exception
    {
        this.rom2 = getComponentManager2().getInstance(RemoteObservationManager.class);

        // Wait until each instance knows the other one
        waitUntil(() -> getMembers(this.rom1).size() == 2 && getMembers(this.rom2).size() == 2);
    }

    /**
     * The answer is updated asynchronously when the members change.
     */
    private void assertSingletonNode(boolean expected1, boolean expected2) throws InterruptedException
    {
        waitUntil(() -> this.singletonManager1.isSingletonNode() == expected1
            && this.singletonManager2.isSingletonNode() == expected2);
    }

    private Collection<NetworkMember> getMembers(RemoteObservationManager rom)
    {
        return rom.getChannels().iterator().next().getMembers();
    }

    private void waitUntil(BooleanSupplier condition) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + TIMEOUT;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for the cluster");
            }
            TimeUnit.MILLISECONDS.sleep(100);
        }
    }

    private EventListener addChangeListener(ObservationManager observationManager)
    {
        EventListener listener = mock(EventListener.class);
        when(listener.getName()).thenReturn("singletonListener");
        when(listener.getEvents()).thenReturn(List.of(new ClusterSingletonNodeChangedEvent()));
        observationManager.addListener(listener);

        return listener;
    }

    @Test
    void oldestMemberWinsAndLeaves() throws Exception
    {
        startInstances();

        // Same (default) priority: the oldest member, which is also the leader, is the singleton node
        assertSingletonNode(true, false);
        assertTrue(this.rom1.getChannels().iterator().next().getLeader().isLocal());

        EventListener listener2 = addChangeListener(getObservationManager2());

        // The singleton node leaves the cluster: the other member takes over and is notified
        this.rom1.stopChannel(CHANNEL);

        verify(listener2, timeout(TIMEOUT)).onEvent(any(ClusterSingletonNodeChangedEvent.class),
            same(this.singletonManager2), eq(true));
        assertTrue(this.singletonManager2.isSingletonNode());
    }

    @Test
    void highestPriorityWins() throws Exception
    {
        getConfigurationSource2().setProperty("observation.remote.singletonPriority", 10);

        startInstances();

        // The youngest member has the highest priority
        assertSingletonNode(false, true);

        // Each member knows the priority of the other one
        for (NetworkMember member : getMembers(this.rom1)) {
            assertEquals(member.isLocal() ? 0 : 10, member.getSingletonPriority());
        }
        for (NetworkMember member : getMembers(this.rom2)) {
            assertEquals(member.isLocal() ? 10 : 0, member.getSingletonPriority());
        }

        EventListener listener1 = addChangeListener(getObservationManager1());

        // The member with the highest priority leaves: the other member takes over
        this.rom2.stopChannel(CHANNEL);

        verify(listener1, timeout(TIMEOUT)).onEvent(any(ClusterSingletonNodeChangedEvent.class),
            same(this.singletonManager1), eq(true));
        assertTrue(this.singletonManager1.isSingletonNode());
    }

    @Test
    void negativePriorityNeverWins() throws Exception
    {
        getConfigurationSource1().setProperty("observation.remote.singletonPriority", -1);

        startInstances();

        assertSingletonNode(false, true);

        // Still not the singleton node once alone
        this.rom2.stopChannel(CHANNEL);
        waitUntil(() -> getMembers(this.rom1).size() == 1);
        // Leave time for an (unexpected) update
        TimeUnit.MILLISECONDS.sleep(500);

        assertFalse(this.singletonManager1.isSingletonNode());
    }

    @Test
    void higherPriorityMemberJoins() throws Exception
    {
        getConfigurationSource2().setProperty("observation.remote.singletonPriority", 10);

        // The first instance is alone
        this.rom1 = getComponentManager1().getInstance(RemoteObservationManager.class);
        waitUntil(() -> getMembers(this.rom1).size() == 1);
        assertTrue(this.singletonManager1.isSingletonNode());

        EventListener listener1 = addChangeListener(getObservationManager1());

        // The member with a higher priority joins (a worker coming back): it takes over
        startInstance2();

        verify(listener1, timeout(TIMEOUT)).onEvent(any(ClusterSingletonNodeChangedEvent.class),
            same(this.singletonManager1), eq(false));
        assertSingletonNode(false, true);
    }
}
