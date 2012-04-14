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
 * Convert a local event to a serializable remote event.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Role
public interface LocalEventConverter
{
    /**
     * @return the priority of the converter
     */
    int getPriority();

    /**
     * Convert provided local event to remote event by filling the provided remote event object.
     * 
     * @param localEvent the local event
     * @param remoteEvent the remote event
     * @return if the converter support this conversion it should return true after the conversion, otherwise false
     */
    boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent);
}
