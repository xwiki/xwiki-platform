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

import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkMember;
import org.xwiki.stability.Unstable;

/**
 * A script safe version of {@link NetworkMember} exposing only what authors with just script right are allowed to do.
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
@Unstable
public class SafeNetworkMember implements NetworkMember
{
    private final SafeNetworkChannel channel;

    private final NetworkMember wrapped;

    /**
     * @param channel the channel this member belongs to
     * @param wrapped the wrapped {@link NetworkMember} instance
     */
    public SafeNetworkMember(SafeNetworkChannel channel, NetworkMember wrapped)
    {
        this.channel = channel;
        this.wrapped = wrapped;
    }

    @Override
    public NetworkChannel getChannel()
    {
        return this.channel;
    }

    @Override
    public String getId()
    {
        return this.wrapped.getId();
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
