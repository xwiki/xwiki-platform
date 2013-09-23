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
package org.xwiki.observation.remote.converter;

import org.xwiki.component.annotation.Role;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;

/**
 * Convert a remote event to a local event.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Role
public interface RemoteEventConverter
{
    /**
     * @return the priority of the converter
     */
    int getPriority();

    /**
     * Convert provided remote event to local event by filling the provided local event object.
     * 
     * @param remoteEvent the remote event
     * @param localEvent the local event
     * @return if the converter support this conversion it should return true after the conversion, otherwise false
     */
    boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent);
}
