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
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.protocols.DISCARD;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.configuration.internal.MemoryConfigurationSource;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.internal.jgroups.JGroupsNetworkChannel;
import org.xwiki.observation.remote.test.AbstractROMTestCase;
import org.xwiki.test.annotation.AllComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test of the detection of the cluster members which left for good, between two instances connected with
 * a JGroups TCP stack. The stacks of this test use their own ports, so that members started by other tests of the same
 * JVM (which can take some time to leave) never join the cluster of this test.
 *
 * @version $Id$
 */
@AllComponents
class ClusterMemberDepartureTCPROMTest extends AbstractROMTestCase
{
    private static final String CHANNEL = "departuretcp";

    private static final String INSTANCE1 = "instance1";

    private static final String INSTANCE2 = "instance2";

    private static final long TIMEOUT = 20000;

    private String previousBindAddress;

    private RemoteObservationManager rom1;

    private RemoteObservationManager rom2;

    private ClusterMemberDepartureManager departureManager1;

    @Override
    @BeforeEach
    public void beforeEach() throws Exception
    {
        super.beforeEach();

        this.previousBindAddress = System.setProperty("jgroups.bind_addr", "localhost");

        getConfigurationSource1().setProperty("observation.remote.channels", List.of(CHANNEL));
        getConfigurationSource2().setProperty("observation.remote.channels", List.of(CHANNEL));

        // Make sure each instance has its own identifier (the members are indexed by identifier)
        setId(getComponentManager1(), getConfigurationSource1(), INSTANCE1);
        setId(getComponentManager2(), getConfigurationSource2(), INSTANCE2);

        this.departureManager1 = getComponentManager1().getInstance(ClusterMemberDepartureManager.class);
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

    private JGroupsNetworkChannel getChannel(RemoteObservationManager rom)
    {
        return (JGroupsNetworkChannel) rom.getChannels().iterator().next();
    }

    private void startInstances() throws Exception
    {
        this.rom1 = getComponentManager1().getInstance(RemoteObservationManager.class);
        this.rom2 = getComponentManager2().getInstance(RemoteObservationManager.class);

        // Wait until each instance knows the other one
        waitUntil(() -> getMembers(this.rom1).size() == 2 && getMembers(this.rom2).size() == 2);
        // The first instance knows the second one from the network event
        waitUntil(() -> this.departureManager1.getMemberIds().equals(Set.of(INSTANCE1, INSTANCE2)));
    }

    private Collection<NetworkMember> getMembers(RemoteObservationManager rom)
    {
        Collection<NetworkChannel> channels = rom.getChannels();

        return channels.isEmpty() ? List.of() : channels.iterator().next().getMembers();
    }

    private void waitUntil(BooleanSupplier condition) throws InterruptedException
    {
        long deadline = System.currentTimeMillis() + TIMEOUT;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                fail("Timed out waiting for the cluster");
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
    }

    private EventListener addDepartedListener(ObservationManager observationManager)
    {
        EventListener listener = mock(EventListener.class);
        when(listener.getName()).thenReturn("departedListener");
        when(listener.getEvents()).thenReturn(List.of(new ClusterMemberDepartedEvent()));
        observationManager.addListener(listener);

        return listener;
    }

    @Test
    void memberLeavesForGood() throws Exception
    {
        getConfigurationSource1().setProperty("observation.remote.reclaimDelay", 1L);

        startInstances();

        EventListener listener1 = addDepartedListener(getObservationManager1());

        long leftTime = System.currentTimeMillis();
        this.rom2.stopChannel(CHANNEL);

        verify(listener1, timeout(TIMEOUT)).onEvent(any(ClusterMemberDepartedEvent.class),
            same(this.departureManager1), eq(INSTANCE2));

        // Not before the delay
        assertTrue(System.currentTimeMillis() - leftTime >= 1000);
        assertTrue(this.departureManager1.isDeparted(INSTANCE2));
        assertNotNull(this.departureManager1.getDepartureDate(INSTANCE2));
        assertEquals(Set.of(INSTANCE1), this.departureManager1.getMemberIds());
    }

    @Test
    void memberComesBackBeforeTheDelay() throws Exception
    {
        // Long enough for the second instance to restart its channel (about 1 second), short enough for the test
        long delay = 5;
        getConfigurationSource1().setProperty("observation.remote.reclaimDelay", delay);

        startInstances();

        EventListener listener1 = addDepartedListener(getObservationManager1());

        this.rom2.stopChannel(CHANNEL);
        waitUntil(() -> this.departureManager1.getDepartureDate(INSTANCE2) != null);
        long leftTime = this.departureManager1.getDepartureDate(INSTANCE2).getTime();

        // The second instance comes back
        this.rom2.startChannel(CHANNEL);
        waitUntil(() -> this.departureManager1.getMemberIds().contains(INSTANCE2));
        assertTrue(System.currentTimeMillis() - leftTime < delay * 1000,
            "The second instance took longer than the delay to come back");
        assertNull(this.departureManager1.getDepartureDate(INSTANCE2));
        assertFalse(this.departureManager1.isDeparted(INSTANCE2));

        // Wait until the end of the delay: the departure is not notified
        long remaining = leftTime + delay * 1000 + 500 - System.currentTimeMillis();
        if (remaining > 0) {
            TimeUnit.MILLISECONDS.sleep(remaining);
        }
        verify(listener1, never()).onEvent(any(), any(), any());
        assertFalse(this.departureManager1.isDeparted(INSTANCE2));
    }

    /**
     * A network partition shorter than the delay: the members see each other leave, then the partition heals and the
     * views are merged. The member which came back through the merge must not be considered departed.
     */
    @Test
    void partitionShorterThanTheDelay() throws Exception
    {
        // A stack with a fast failure detection and fast merges, and a protocol to drop the messages
        String channel = "partitiontcp";
        getConfigurationSource1().setProperty("observation.remote.channels", List.of(channel));
        getConfigurationSource2().setProperty("observation.remote.channels", List.of(channel));
        long delay = 15;
        getConfigurationSource1().setProperty("observation.remote.reclaimDelay", delay);

        startInstances();

        EventListener listener1 = addDepartedListener(getObservationManager1());

        JChannel jchannel1 = getChannel(this.rom1).getJChannel();
        Address address2 = getChannel(this.rom2).getJChannel().getAddress();
        DISCARD discard1 = jchannel1.getProtocolStack().findProtocol(DISCARD.class);

        // Partition: the first instance neither sends nor receives anything until it sees the second one leave
        discard1.discardAll(true);
        waitUntil(() -> this.departureManager1.getDepartureDate(INSTANCE2) != null);
        long leftTime = this.departureManager1.getDepartureDate(INSTANCE2).getTime();

        // Heal the partition and wait for the merge
        discard1.discardAll(false);
        waitUntil(() -> jchannel1.getView().containsMember(address2));
        assertTrue(System.currentTimeMillis() - leftTime < delay * 1000,
            "The partition took longer than the delay to heal");

        // Wait until the end of the delay: the member is back, it's not departed
        long remaining = leftTime + delay * 1000 + 1000 - System.currentTimeMillis();
        if (remaining > 0) {
            TimeUnit.MILLISECONDS.sleep(remaining);
        }
        assertTrue(jchannel1.getView().containsMember(address2));
        assertFalse(this.departureManager1.isDeparted(INSTANCE2));
        assertNull(this.departureManager1.getDepartureDate(INSTANCE2));
        verify(listener1, never()).onEvent(any(), any(), any());
    }

    /**
     * Two members share the same identifier: the older one, which is the cluster singleton node, still notifies the
     * departures, and the younger one postpones them since it can't identify the older one.
     */
    @Test
    void sharedIdentifier() throws Exception
    {
        String sharedId = "shared";
        setId(getComponentManager1(), getConfigurationSource1(), sharedId);
        setId(getComponentManager2(), getConfigurationSource2(), sharedId);
        getConfigurationSource1().setProperty("observation.remote.reclaimDelay", 1L);
        getConfigurationSource2().setProperty("observation.remote.reclaimDelay", 1L);
        ClusterMemberDepartureManager departureManager2 =
            getComponentManager2().getInstance(ClusterMemberDepartureManager.class);

        this.rom1 = getComponentManager1().getInstance(RemoteObservationManager.class);
        this.rom2 = getComponentManager2().getInstance(RemoteObservationManager.class);
        // Wait until the older instance knows the identifier of the younger one
        waitUntil(() -> getChannel(this.rom2).getJChannel().getView().size() == 2 && getChannel(this.rom1)
            .getViewMembers().stream().anyMatch(member -> !member.isLocal() && sharedId.equals(member.getId())));

        assertEquals(Set.of(sharedId), this.departureManager1.getMemberIds());
        assertEquals(Set.of(sharedId), departureManager2.getMemberIds());

        EventListener listener1 = addDepartedListener(getObservationManager1());
        EventListener listener2 = addDepartedListener(getObservationManager2());

        long watchTime = System.currentTimeMillis();
        this.departureManager1.watch(List.of("ghost"));
        departureManager2.watch(List.of("ghost"));

        verify(listener1, timeout(TIMEOUT)).onEvent(any(ClusterMemberDepartedEvent.class),
            same(this.departureManager1), eq("ghost"));
        assertTrue(this.departureManager1.isDeparted("ghost"));

        // Leave the younger instance the time to (not) notify it
        long remaining = watchTime + 2000 - System.currentTimeMillis();
        if (remaining > 0) {
            TimeUnit.MILLISECONDS.sleep(remaining);
        }
        assertFalse(departureManager2.isDeparted("ghost"));
        verify(listener2, never()).onEvent(any(), any(), any());
    }
}
