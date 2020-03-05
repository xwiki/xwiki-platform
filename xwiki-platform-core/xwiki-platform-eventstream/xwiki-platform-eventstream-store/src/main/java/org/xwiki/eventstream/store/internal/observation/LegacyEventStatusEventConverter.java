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
package org.xwiki.eventstream.store.internal.observation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.internal.events.EventStatusAddOrUpdatedEvent;
import org.xwiki.eventstream.internal.events.EventStatusDeletedEvent;
import org.xwiki.eventstream.store.internal.LegacyEvent;
import org.xwiki.eventstream.store.internal.LegacyEventLoader;
import org.xwiki.eventstream.store.internal.LegacyEventStatus;
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
@Named("eventstatus")
public class LegacyEventStatusEventConverter extends AbstractEventConverter
{
    private static final Set<Class<? extends Event>> EVENTS =
        new HashSet<>(Arrays.asList(EventStatusAddOrUpdatedEvent.class, EventStatusDeletedEvent.class));

    private static final String PROP_EVENTID = "eventId";

    private static final String PROP_ENTITYID = "entityId";

    private static final String PROP_ISREAD = "isRead";

    @Inject
    private LegacyEventLoader loader;

    @Inject
    private Logger logger;

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource(serializeLegacyEventStatus((LegacyEventStatus) localEvent.getSource()));

            return true;
        }

        return false;
    }

    private Serializable serializeLegacyEventStatus(LegacyEventStatus local)
    {
        Serializable remote = null;

        if (local != null) {
            Map<String, Object> map = new HashMap<>();

            map.put(PROP_EVENTID, local.getActivityEvent().getEventId());
            map.put(PROP_ENTITYID, local.getEntityId());
            map.put(PROP_ISREAD, local.isRead());

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
                localEvent.setSource(unserializeLegacyEventStatus(remoteEvent.getSource()));

                return true;
            } catch (Exception e) {
                this.logger.error("Failed to convert remote event [{}]", remoteEvent, e);
            }
        }

        return false;
    }

    private Object unserializeLegacyEventStatus(Serializable remote) throws QueryException
    {
        if (remote instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) remote;

            String eventId = (String) map.get(PROP_EVENTID);

            LegacyEvent event = this.loader.getLegacyEvent(eventId);

            LegacyEventStatus eventStatus = new LegacyEventStatus();

            eventStatus.setActivityEvent(event);
            eventStatus.setEntityId((String) map.get(PROP_ENTITYID));
            eventStatus.setRead((Boolean) map.get(PROP_ISREAD));

            return eventStatus;
        }

        return null;
    }
}
