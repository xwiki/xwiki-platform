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
import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.stack.AddressGenerator;
import org.jgroups.util.ExtendedUUID;
import org.xwiki.observation.remote.NetworkMember;

/**
 * JGroups implementation of {@link NetworkMember}.
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
public class JGroupsNetworkMember implements NetworkMember
{
    /**
     * The key under which the singleton priority of a member is stored in its JGroups address.
     *
     * @since 18.10.0RC1
     */
    public static final String SINGLETON_PRIORITY_KEY = "xwiki.singletonPriority";

    private final JGroupsNetworkChannel channel;

    private final String id;

    private final Address address;

    /**
     * @param channel the channel this member belongs to
     * @param id the identifier of the member from XWiki point of view
     * @param address the identifier of the member from JGroups point of view
     */
    public JGroupsNetworkMember(JGroupsNetworkChannel channel, String id, Address address)
    {
        this.channel = channel;
        this.id = id;
        this.address = address;
    }

    @Override
    public JGroupsNetworkChannel getChannel()
    {
        return this.channel;
    }

    @Override
    public String getId()
    {
        return this.id;
    }

    /**
     * @return the identifier of the member from JGroups point of view
     */
    public Address getAddress()
    {
        return this.address;
    }

    /**
     * Generates a JGroups address carrying the passed singleton priority, so that the other members know it as soon as
     * they know the address of the member (see {@link #getSingletonPriority()}).
     *
     * @param name the logical name of the member, or {@code null} if none is known yet
     * @param singletonPriority the singleton priority of the member
     * @return the new address
     * @since 18.10.0RC1
     */
    public static Address createAddress(String name, int singletonPriority)
    {
        // Stored as text so that it's readable when JGroups prints the address
        return ExtendedUUID.randomUUID(name).put(SINGLETON_PRIORITY_KEY,
            String.valueOf(singletonPriority).getBytes(StandardCharsets.US_ASCII));
    }

    /**
     * Creates the generator of the JGroups addresses of a channel, carrying the passed singleton priority.
     *
     * @param singletonPriority the singleton priority of the current instance
     * @return the address generator to install on the channel before connecting it
     * @since 18.10.0RC1
     */
    public static AddressGenerator createAddressGenerator(int singletonPriority)
    {
        return new AddressGenerator()
        {
            @Override
            @Deprecated
            public Address generateAddress()
            {
                return generateAddress(null);
            }

            @Override
            public Address generateAddress(String name)
            {
                return createAddress(name, singletonPriority);
            }
        };
    }

    @Override
    public boolean isLocal()
    {
        JChannel jchannel = this.channel.getJChannel();

        return jchannel != null && this.address.equals(jchannel.getAddress());
    }

    @Override
    public int getSingletonPriority()
    {
        if (this.address instanceof ExtendedUUID extendedAddress) {
            byte[] value = extendedAddress.get(SINGLETON_PRIORITY_KEY);
            if (value != null) {
                try {
                    return Integer.parseInt(new String(value, StandardCharsets.US_ASCII));
                } catch (NumberFormatException e) {
                    // Not a value set by XWiki, ignore it
                }
            }
        }

        // A member which did not configure any priority (or which runs an older version) uses a standard address
        return 0;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof JGroupsNetworkMember otherMember) {
            return Objects.equals(otherMember.getId(), getId())
                && otherMember.getAddress().compareTo(getAddress()) == 0;
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        HashCodeBuilder builder = new HashCodeBuilder();

        builder.append(getId());
        builder.append(getAddress());

        return builder.build();
    }

    @Override
    public String toString()
    {
        return getId() + ':' + getAddress();
    }
}
