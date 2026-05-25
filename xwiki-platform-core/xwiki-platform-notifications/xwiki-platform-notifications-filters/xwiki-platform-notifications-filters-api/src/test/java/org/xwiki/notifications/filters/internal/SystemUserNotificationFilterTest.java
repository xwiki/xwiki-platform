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
package org.xwiki.notifications.filters.internal;

import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class SystemUserNotificationFilterTest
{
    @InjectMockComponents
    private SystemUserNotificationFilter filter;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Event nonSystemEvent;

    private Event systemEvent;

    private DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "alice");

    @BeforeEach
    void beforeEach()
    {
        DocumentReference systemUserReference = new DocumentReference("xwiki", "XWiki", "superadmin");
        DocumentReference randomUserReference = new DocumentReference("xwiki", "XWiki", "bob");

        when(this.entityReferenceSerializer.serialize(systemUserReference.getLocalDocumentReference()))
            .thenReturn("serializedSystemUser");

        this.nonSystemEvent = mock(Event.class);
        when(this.nonSystemEvent.getUser()).thenReturn(randomUserReference);

        this.systemEvent = mock(Event.class);
        when(this.systemEvent.getUser()).thenReturn(systemUserReference);
    }

    @Test
    void filterEvent()
    {
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.filter.filterEvent(this.nonSystemEvent, this.randomUser, List.of(), NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.filter.filterEvent(this.systemEvent, this.randomUser, List.of(), NotificationFormat.ALERT));
    }

    @Test
    void filterTargetedSystemEvent()
    {
        when(this.systemEvent.getTarget()).thenReturn(Set.of("user1", "user2"));

        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.filter.filterEvent(this.systemEvent, this.randomUser, List.of(), NotificationFormat.ALERT));
    }

    @Test
    void filterExpression()
    {
        NotificationPreference fakePreference = mock(NotificationPreference.class);

        assertNull(this.filter.filterExpression(this.randomUser, List.of(), fakePreference));
        assertEquals("USER <> \"serializedSystemUser\"", this.filter
            .filterExpression(this.randomUser, List.of(), NotificationFilterType.EXCLUSIVE, null).toString());
    }

    @Test
    void matchesPreference()
    {
        assertFalse(this.filter.matchesPreference(mock(NotificationPreference.class)));
    }

    @Test
    void getName() throws Exception
    {
        assertEquals(SystemUserNotificationFilter.FILTER_NAME, this.filter.getName());
    }
}
