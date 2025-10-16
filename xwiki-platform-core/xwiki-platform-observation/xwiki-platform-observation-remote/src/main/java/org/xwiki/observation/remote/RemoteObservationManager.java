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

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Provide apis to manage the event network interface.
 *
 * @version $Id$
 * @since 2.0M3
 */
@Role
public interface RemoteObservationManager
{
    /**
     * @return the configured {@link NetworkAdapter}
     * @since 17.10.0RC1
     */
    @Unstable
    NetworkAdapter getNetworkAdapter();

    /**
     * Send an event in the different network channels.
     * <p>
     * This method is not supposed to be used directly for a new event unless the user specifically want to bypass or
     * emulate {@link org.xwiki.observation.ObservationManager}.
     *
     * @param localEvent the event to send
     */
    void notify(LocalEventData localEvent);

    /**
     * Send an event to the target channel members.
     * 
     * @param localEvent the event to send
     * @param targets the members to send the event to
     * @since 17.10.0RC1
     */
    @Unstable
    void notify(LocalEventData localEvent, List<NetworkMember> targets);

    /**
     * Inject an remote event in the local {@link org.xwiki.observation.ObservationManager}.
     * <p>
     * This method is not supposed to be used directly for a new event unless the user specifically want to bypass or
     * emulate network.
     *
     * @param remoteEvent the event
     */
    void notify(RemoteEventData remoteEvent);

    /**
     * Stop a running channel.
     *
     * @param channelId the identifier of the channel to stop
     * @throws RemoteEventException error when trying to stop a running channel
     */
    void stopChannel(String channelId) throws RemoteEventException;

    /**
     * Start a channel.
     *
     * @param channelId the identifier of the channel to start
     * @throws RemoteEventException error when trying to start a channel
     */
    void startChannel(String channelId) throws RemoteEventException;

    /**
     * @return the channels used to communicate with other XWiki instances
     * @since 17.9.0RC1
     */
    @Unstable
    default Collection<NetworkChannel> getChannels()
    {
        return List.of();
    }
}
