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
package org.xwiki.eventstream.store.internal;

<<<<<<< Updated upstream
import org.xwiki.component.annotation.Role;
=======
import java.net.MalformedURLException;
import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
>>>>>>> Stashed changes
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
<<<<<<< Updated upstream
=======
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
>>>>>>> Stashed changes

/**
 * Helper that convert some objects from the Event Stream module to objects of the Activity Stream module
 * (which is used for the storage) and the opposite.
 *
 * @version $Id$
 * @since 12.1RC1
 */
@Role
public interface LegacyEventConverter
{
    /**
     * Converts a new {@link Event} to the old {@link LegacyEvent}.
     *
     * @param e the event to transform
     * @return the equivalent activity event
     */
    LegacyEvent convertEventToLegacyActivity(Event e);

    /**
     * Convert an old {@link LegacyEvent} to the new {@link Event}.
     *
     * @param e the activity event to transform
     * @return the equivalent event
     */
    Event convertLegacyActivityToEvent(LegacyEvent e);

    /**
     * Convert an {@link EventStatus} to an {@link LegacyEventStatus}.
     *
     * @param eventStatus the status to transform
     * @return the equivalent activity event status
     */
<<<<<<< Updated upstream
    LegacyEventStatus convertEventStatusToLegacyActivityStatus(EventStatus eventStatus);
=======
    public LegacyEventStatus convertEventStatusToLegacyActivityStatus(EventStatus eventStatus)
    {
        LegacyEventStatus legacyEventStatus = new LegacyEventStatus();
        legacyEventStatus.setActivityEvent(convertEventToLegacyActivity(eventStatus.getEvent()));
        legacyEventStatus.setEntityId(eventStatus.getEntityId());
        legacyEventStatus.setRead(eventStatus.isRead());

        return legacyEventStatus;
    }
>>>>>>> Stashed changes

    /**
     * Convert an {@link LegacyEventStatus} to an {@link EventStatus}.
     *
     * @param eventStatus the activity event status to transform
     * @return the equivalent event status
     */
    EventStatus convertLegacyActivityStatusToEventStatus(LegacyEventStatus eventStatus);
}
