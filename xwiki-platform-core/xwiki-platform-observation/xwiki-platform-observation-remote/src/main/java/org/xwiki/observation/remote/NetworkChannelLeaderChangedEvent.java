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

import org.xwiki.observation.event.Event;
import org.xwiki.stability.Unstable;

/**
 * This event is triggered when the leader of a {@link NetworkChannel} changes (generally because the previous leader
 * left the cluster). It's also triggered when the identifier of the current leader becomes known, since the leader can
 * become known before its identifier (see {@link NetworkChannel#getLeader()}).
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the {@link NetworkChannel}</li>
 * <li>data: the new leader as a {@link NetworkMember}, whose identifier is {@code null} while it's not known</li>
 * </ul>
 * 
 * @version $Id$
 * @since 17.9.0RC1
 */
@Unstable
public class NetworkChannelLeaderChangedEvent implements Event
{
    private final String channel;

    /**
     * Listen to all channels.
     */
    public NetworkChannelLeaderChangedEvent()
    {
        this(null);
    }

    /**
     * @param channel the identifier of the impacted channel
     */
    public NetworkChannelLeaderChangedEvent(String channel)
    {
        this.channel = channel;
    }

    /**
     * @return the channel
     */
    public String getChannel()
    {
        return channel;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        return otherEvent instanceof NetworkChannelLeaderChangedEvent leaderEvent
            && (getChannel() == null || getChannel().equals(leaderEvent.getChannel()));
    }
}
