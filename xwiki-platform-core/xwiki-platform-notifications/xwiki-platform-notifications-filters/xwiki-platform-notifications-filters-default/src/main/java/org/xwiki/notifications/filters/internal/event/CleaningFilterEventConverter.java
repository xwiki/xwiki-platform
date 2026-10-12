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

import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.filters.event.CleaningFilterEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.AbstractEventConverter;
import org.xwiki.observation.remote.converter.LocalEventConverter;

/**
 * Keep {@link CleaningFilterEvent} on the cluster member where it is notified.
 * <p>
 * The event is notified before filter preferences are deleted, so that a listener can cancel the deletion, which only
 * makes sense on the member performing it. The other members are informed of the deletion itself by
 * {@link NotificationFilterPreferenceDeletedEvent}. Also, the data of the event (the filter preferences about to be
 * deleted) cannot be serialized.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Named("cleaningfilter")
@Singleton
public class CleaningFilterEventConverter implements LocalEventConverter
{
    @Override
    public int getPriority()
    {
        // Must be called before the generic serializable converter, which would accept the event.
        return AbstractEventConverter.DEFAULT_PRIORITY;
    }

    @Override
    public boolean toRemote(LocalEventData localEvent, RemoteEventData remoteEvent)
    {
        // Handle the event without filling the remote event, so that it is not sent to the other members.
        return localEvent.getEvent() instanceof CleaningFilterEvent;
    }
}
