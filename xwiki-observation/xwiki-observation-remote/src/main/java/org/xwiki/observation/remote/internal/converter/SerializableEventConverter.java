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
package org.xwiki.observation.remote.internal.converter;

import java.io.Serializable;

import org.xwiki.component.annotation.Component;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * Default implementation of {@link LocalEventConverter}. Support any serializable event.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class SerializableEventConverter extends AbstractEventConverter
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.AbstractEventConverter#getPriority()
     */
    public int getPriority()
    {
        // SerializableEventConverter is used only if no other converter could be found so we make sure it has a very
        // low priority
        return 2000;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.RemoteEventConverter#fromRemote(org.xwiki.observation.remote.RemoteEventData,
     *      org.xwiki.observation.remote.LocalEventData)
     */
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (remoteEvent.getEvent() instanceof Event) {
            localEvent.setEvent((Event) remoteEvent.getEvent());

            if (remoteEvent.getSource() != null) {
                localEvent.setSource(remoteEvent.getSource());
            }

            if (remoteEvent.getData() != null) {
                localEvent.setData(remoteEvent.getData());
            }

            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.remote.converter.LocalEventConverter#toRemote(org.xwiki.observation.remote.LocalEventData,
     *      org.xwiki.observation.remote.RemoteEventData)
     */
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof Serializable) {
            remoteEvent.setEvent((Serializable) localEvent.getEvent());

            if (localEvent.getSource() instanceof Serializable) {
                remoteEvent.setSource((Serializable) localEvent.getSource());
            }

            if (localEvent.getData() instanceof Serializable) {
                remoteEvent.setData((Serializable) localEvent.getData());
            }

            return true;
        }

        return false;
    }
}
