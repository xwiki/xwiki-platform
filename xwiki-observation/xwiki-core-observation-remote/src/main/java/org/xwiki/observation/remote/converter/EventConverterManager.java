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
 *
 */
package org.xwiki.observation.remote.converter;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;

/**
 * Provide events converters.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@ComponentRole
public interface EventConverterManager
{
    /**
     * @return the local to remote events converters
     */
    List<LocalEventConverter> getLocalEventConverters();

    /**
     * @return the remote to local events converters
     */
    List<RemoteEventConverter> getRemoteEventConverters();

    /**
     * Convert a local event to a remote event.
     * 
     * @param localEvent the local event
     * @return the remote event, if null the event should not be sent to the network.
     */
    RemoteEventData createRemoteEventData(LocalEventData localEvent);

    /**
     * Convert a remote event to a local event.
     * 
     * @param remoteEvent the remote event
     * @return the local event, if null the event should not send to {@link org.xwiki.observation.ObservationManager}.
     */
    LocalEventData createLocalEventData(RemoteEventData remoteEvent);
}
