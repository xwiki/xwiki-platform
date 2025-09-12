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

import javax.inject.Inject;

import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.NetworkChannel;
import org.xwiki.observation.remote.NetworkChannelLeaderChangedEvent;
import org.xwiki.observation.remote.NetworkMemberJoinedEvent;
import org.xwiki.observation.remote.NetworkMemberLeftEvent;

/**
 * Takes care of notifications related to channels.
 * 
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component(roles = JGroupsNetworkChannelNotifier.class)
@Singleton
public class JGroupsNetworkChannelNotifier
{
    @Inject
    private ObservationManager observation;

    /**
     * @param channel the channel
     */
    public void notifyChannelLeaderChanged(NetworkChannel channel)
    {
        this.observation.notify(new NetworkChannelLeaderChangedEvent(channel.getId()), channel, channel.getLeader());
    }

    /**
     * @param channel the channel
     * @param member the new member
     */
    public void notifyChannelMemberJoined(NetworkChannel channel, JGroupsNetworkMember member)
    {
        this.observation.notify(new NetworkMemberJoinedEvent(channel.getId()), channel, member);
    }

    /**
     * @param channel the channel
     * @param member the old member
     */
    public void notifychannelMemberLeft(NetworkChannel channel, JGroupsNetworkMember member)
    {
        this.observation.notify(new NetworkMemberLeftEvent(channel.getId()), channel, member);
    }
}
