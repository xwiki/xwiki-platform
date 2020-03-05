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
package org.xwiki.eventstream.internal.observation;

import java.io.Serializable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.events.AbstractEventStreamEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;
import org.xwiki.query.QueryException;

/**
 * Convert all event status events to remote events and back to local events.
 *
 * @version $Id$
 * @since 12.2RC1
 * @since 11.10.4
 */
@Component
@Singleton
@Named("eventstreamevent")
public class EventStreamEventConverter extends AbstractEventConverter
{
    @Inject
    private EventStream stream;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (localEvent.getEvent() instanceof AbstractEventStreamEvent) {
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource(serializeEvent((org.xwiki.eventstream.Event) localEvent.getSource()));

            return true;
        }

        return false;
    }

    private Serializable serializeEvent(org.xwiki.eventstream.Event local)
    {
        if (local != null) {
            return local.getId();
        }

        return null;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (remoteEvent.getEvent() instanceof AbstractEventStreamEvent) {
            try {
                localEvent.setEvent((Event) remoteEvent.getEvent());
                localEvent.setSource(unserializeEvent(remoteEvent.getSource()));

                return true;
            } catch (Exception e) {
                this.logger.error("Failed to convert remote event [{}]", remoteEvent, e);
            }
        }

        return false;
    }

    private org.xwiki.eventstream.Event unserializeEvent(Serializable remote) throws QueryException
    {
        if (remote instanceof String) {
            return this.stream.getEvent((String) remote);
        }

        return null;
    }
}
