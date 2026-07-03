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

import org.xwiki.stability.Unstable;

/**
 * A channel used to communicate between several XWiki instances.
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
@Unstable
public interface NetworkChannel
{
    /**
     * @return the identifier of the channel
     */
    String getId();

    /**
     * @return the members of the channel
     */
    Collection<NetworkMember> getMembers();

    /**
     * @return the leader of the channel (dynamically changes when a leader becomes offline)
     */
    NetworkMember getLeader();
}
