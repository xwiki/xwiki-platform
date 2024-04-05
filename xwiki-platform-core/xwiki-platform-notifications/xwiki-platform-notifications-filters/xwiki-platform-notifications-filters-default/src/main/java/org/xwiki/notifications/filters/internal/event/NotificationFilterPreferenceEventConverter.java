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
package org.xwiki.notifications.filters.internal.event;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

/**
 * Convert all event status events to remote events and back to local events.
 *
 * @version $Id$
 * @since 12.10.10
 * @since 13.4.4
 * @since 13.8RC1
 */
@Component
@Singleton
@Named("notificationfilterpreference")
public class NotificationFilterPreferenceEventConverter extends AbstractEventConverter
{
    private static final Set<Class<? extends Event>> EVENTS =
        Set.of(NotificationFilterPreferenceAddOrUpdatedEvent.class);

    private static final String PROP_OWNER = "owner";

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        if (EVENTS.contains(localEvent.getEvent().getClass())) {
            remoteEvent.setEvent((Serializable) localEvent.getEvent());
            remoteEvent.setSource(serializePreference(localEvent.getSource()));

            return true;
        }

        return false;
    }

    private Serializable serializePreference(Object local)
    {
        Serializable remote = null;

        if (local instanceof DefaultNotificationFilterPreference) {
            Map<String, Object> map = new HashMap<>();

            map.put(PROP_OWNER, ((DefaultNotificationFilterPreference) local).getOwner());

            remote = (Serializable) map;
        }

        return remote;
    }

    @Override
    public boolean fromRemote(RemoteEventData remoteEvent, LocalEventData localEvent)
    {
        if (EVENTS.contains(remoteEvent.getEvent().getClass())) {
            localEvent.setEvent((Event) remoteEvent.getEvent());
            localEvent.setSource(unserializePreference(remoteEvent.getSource()));

            return true;
        }

        return false;
    }

    private Object unserializePreference(Serializable remote)
    {
        if (remote instanceof Map) {
            Map<String, ?> map = (Map<String, ?>) remote;

            DefaultNotificationFilterPreference local = new DefaultNotificationFilterPreference();
            local.setOwner((String) map.get(PROP_OWNER));

            return local;
        }

        return null;
    }
}
