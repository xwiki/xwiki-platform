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
 * Provide apis to manage the event network interface.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Role
public interface RemoteObservationManager
{
    /**
     * Send a event in the different network channels.
     * <p>
     * This method is not supposed to be used directly for a new event unless the user specifically want to bypass or
     * emulate {@link org.xwiki.observation.ObservationManager}.
     * 
     * @param event the event
     */
    void notify(LocalEventData event);

    /**
     * Inject a remote event in the local {@link org.xwiki.observation.ObservationManager}.
     * <p>
     * This method is not supposed to be used directly for a new event unless the user specifically want to bypass or
     * emulate network.
     * 
     * @param event the event
     */
    void notify(RemoteEventData event);

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
}
