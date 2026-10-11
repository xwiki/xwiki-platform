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
package org.xwiki.observation.remote.internal.jgroups;

import java.nio.charset.StandardCharsets;

import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.ExtendedUUID;
import org.jgroups.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link JGroupsNetworkMember}.
 *
 * @version $Id$
 */
class JGroupsNetworkMemberTest
{
    @Test
    void getSingletonPriority()
    {
        JGroupsNetworkChannel channel = mock(JGroupsNetworkChannel.class);

        assertEquals(10, new JGroupsNetworkMember(channel, "id", JGroupsNetworkMember.createAddress("worker", 10))
            .getSingletonPriority());
        assertEquals(-1, new JGroupsNetworkMember(channel, "id", JGroupsNetworkMember.createAddress(null, -1))
            .getSingletonPriority());

        // Members without priority
        assertEquals(0, new JGroupsNetworkMember(channel, "id", UUID.randomUUID()).getSingletonPriority());
        assertEquals(0, new JGroupsNetworkMember(channel, "id", ExtendedUUID.randomUUID()).getSingletonPriority());

        // Invalid priority
        Address invalid = ExtendedUUID.randomUUID().put(JGroupsNetworkMember.SINGLETON_PRIORITY_KEY,
            "high".getBytes(StandardCharsets.US_ASCII));
        assertEquals(0, new JGroupsNetworkMember(channel, "id", invalid).getSingletonPriority());
    }

    @Test
    void createAddressGenerator()
    {
        JGroupsNetworkChannel channel = mock(JGroupsNetworkChannel.class);
        AddressGenerator generator = JGroupsNetworkMember.createAddressGenerator(10);

        assertEquals(10, new JGroupsNetworkMember(channel, "id", generator.generateAddress("worker"))
            .getSingletonPriority());
        assertEquals(10, new JGroupsNetworkMember(channel, "id", generator.generateAddress()).getSingletonPriority());
    }

    @Test
    void isLocal()
    {
        JGroupsNetworkChannel channel = mock(JGroupsNetworkChannel.class);
        Address localAddress = UUID.randomUUID();
        JGroupsNetworkMember localMember = new JGroupsNetworkMember(channel, "local", localAddress);
        JGroupsNetworkMember remoteMember = new JGroupsNetworkMember(channel, "remote", UUID.randomUUID());

        // Not connected
        assertFalse(localMember.isLocal());

        JChannel jchannel = mock(JChannel.class);
        when(channel.getJChannel()).thenReturn(jchannel);
        when(jchannel.getAddress()).thenReturn(localAddress);

        assertTrue(localMember.isLocal());
        assertFalse(remoteMember.isLocal());

        // Same identifier but another address
        assertFalse(new JGroupsNetworkMember(channel, "local", UUID.randomUUID()).isLocal());
    }
}
