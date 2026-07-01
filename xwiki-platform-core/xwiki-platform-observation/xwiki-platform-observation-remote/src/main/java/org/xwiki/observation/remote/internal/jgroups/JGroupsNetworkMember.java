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

import java.util.Objects;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jgroups.Address;
import org.xwiki.observation.remote.NetworkMember;

/**
 * JGroups implementation of {@link NetworkMember}.
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
public class JGroupsNetworkMember implements NetworkMember
{
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
