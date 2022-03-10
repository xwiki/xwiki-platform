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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.events.MailEntityAddedEvent;
import org.xwiki.eventstream.events.MailEntityDeleteEvent;
import org.xwiki.eventstream.internal.DefaultEntityEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * Convert all mail entity events to remote events and back to local events.
 *
 * @version $Id$
 * @since 12.10
 * @since 12.6.5
 */
@Component
@Singleton
@Named("mailentity")
public class MailEntityEventConverter extends AbstractEventConverter
{
    private static final Set<Class<? extends Event>> EVENTS =
        new HashSet<>(Arrays.asList(MailEntityAddedEvent.class, MailEntityDeleteEvent.class));

    private static final String PROP_EVENTID = "eventId";

    private static final String PROP_ENTITYID = "entityId";

    @Inject
    private EventStore store;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource(serializeEntityEvent((EntityEvent) localEvent.getSource()));

            return true;
        }

        return false;
    }

    private Serializable serializeEntityEvent(EntityEvent local)
    {
        Serializable remote = null;

        if (local != null) {
            Map<String, Object> map = new HashMap<>();

            map.put(PROP_EVENTID, local.getEvent().getId());
            map.put(PROP_ENTITYID, local.getEntityId());

            remote = (Serializable) map;
        }

        return remote;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (EVENTS.contains(remoteEvent.getEvent().getClass())) {
            try {
                localEvent.setEvent((Event) remoteEvent.getEvent());
                localEvent.setSource(unserializeEntityEvent(remoteEvent.getSource()));

                return true;
            } catch (Exception e) {
                this.logger.error("Failed to convert remote event [{}]", remoteEvent, e);
            }
        }

        return false;
    }

    private Object unserializeEntityEvent(Serializable remote) throws EventStreamException
    {
        if (remote instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) remote;

            String eventId = (String) map.get(PROP_EVENTID);

            Optional<org.xwiki.eventstream.Event> event = this.store.getEvent(eventId);

            if (event.isPresent()) {
                return new DefaultEntityEvent(event.get(), (String) map.get(PROP_ENTITYID));
            } else {
                this.logger.warn("Could not find any event corresponding to the remote event with id [{}]", eventId);
            }
        }

        return null;
    }
}
