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

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.event.CleaningFilterEvent;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.EventConverterManager;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.observation.remote.internal.converter.DefaultEventConverterManager;
import org.xwiki.observation.remote.internal.converter.EventConverters;
import org.xwiki.observation.remote.internal.converter.SerializableEventConverter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link CleaningFilterEventConverter}, together with the generic converters of the remote observation.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    DefaultEventConverterManager.class,
    EventConverters.class,
    SerializableEventConverter.class,
    CleaningFilterEventConverter.class
})
class CleaningFilterEventConverterTest
{
    private static final DocumentReference USER = new DocumentReference("wiki", "XWiki", "User");

    private static final DocumentReference DELETED_DOCUMENT = new DocumentReference("wiki", "Space", "Page");

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private EventConverterManager converterManager;

    private LocalEventData cleaningEvent;

    @BeforeEach
    void beforeEach() throws Exception
    {
        this.converterManager = this.componentManager.getInstance(EventConverterManager.class);

        // The data notified by DeletedDocumentCleanUpFilterProcessingQueue: the preferences about to be deleted.
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();
        preference.setId(NotificationFilterPreference.DB_ID_FILTER_PREFIX + "42");
        preference.setOwner("wiki:XWiki.User");
        preference.setPageOnly("wiki:Space.Page");
        Set<NotificationFilterPreference> preferences = Set.of(preference);

        this.cleaningEvent = new LocalEventData(new CleaningFilterEvent(), DELETED_DOCUMENT, preferences);
    }

    @Test
    void toRemote() throws Exception
    {
        LocalEventConverter converter =
            this.componentManager.getInstance(LocalEventConverter.class, "cleaningfilter");
        RemoteEventData remoteEvent = new RemoteEventData();

        assertTrue(converter.toRemote(this.cleaningEvent, remoteEvent));
        assertNull(remoteEvent.getEvent());
        assertNull(remoteEvent.getSource());
        assertNull(remoteEvent.getData());

        assertFalse(converter.toRemote(
            new LocalEventData(new NotificationFilterPreferenceDeletedEvent(), USER, Set.of("NFP_42")), remoteEvent));
        assertNull(remoteEvent.getEvent());
    }

    @Test
    void cleaningFilterEventIsNotSentToTheOtherMembers()
    {
        assertNull(this.converterManager.createRemoteEventData(this.cleaningEvent));
    }
}
