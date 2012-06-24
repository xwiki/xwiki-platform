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

import org.xwiki.component.annotation.Role;

/**
 * Handle all the actual communication with the network.
 * <p>
 * It's the entry point of the chosen implementation for the actual event distribution.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Role
public interface NetworkAdapter
{
    /**
     * Send serializable event to the network depending of the implementation.
     * 
     * @param remoteEvent the serializable event to send
     */
    void send(RemoteEventData remoteEvent);

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
     * Stop all running channels.
     * 
     * @throws RemoteEventException error when trying to stop a running channel
     * @since 2.3M1
     */
    void stopAllChannels() throws RemoteEventException;
}
