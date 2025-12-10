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
package org.xwiki.observation.remote.script;

import java.util.Collection;

import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.stability.Unstable;

/**
 * A script safe version of {@link NetworkChannel} exposing only what authors with just script right are allowed to do.
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
@Unstable
public class SafeNetworkChannel implements NetworkChannel
{
    private final NetworkChannel wrapped;

    /**
     * @param wrapped the wrapped {@link NetworkChannel} instance
     */
    public SafeNetworkChannel(NetworkChannel wrapped)
    {
        this.wrapped = wrapped;
    }

    // NetworkChannel

    @Override
    public String getId()
    {
        return this.wrapped.getId();
    }

    @Override
    public Collection<NetworkMember> getMembers()
    {
        return this.wrapped.getMembers().stream().<NetworkMember>map(m -> new SafeNetworkMember(this, m)).toList();
    }

    @Override
    public NetworkMember getLeader()
    {
        return new SafeNetworkMember(this, this.wrapped.getLeader());
    }

    // Object

    @Override
    public int hashCode()
    {
        return this.wrapped.hashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        return this.wrapped.equals(obj);
    }

    @Override
    public String toString()
    {
        return this.wrapped.toString();
    }
}
