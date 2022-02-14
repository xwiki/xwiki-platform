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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

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
    void beforeEach() throws Exception
    {
        DocumentReference systemUserReference = new DocumentReference("xwiki", "XWiki", "superadmin");
        DocumentReference randomUserReference = new DocumentReference("xwiki", "XWiki", "bob");

        when(this.entityReferenceSerializer.serialize(systemUserReference.getLocalDocumentReference()))
            .thenReturn("serializedSystemUser");

        nonSystemEvent = mock(Event.class);
        when(this.nonSystemEvent.getUser()).thenReturn(randomUserReference);

        systemEvent = mock(Event.class);
        when(this.systemEvent.getUser()).thenReturn(systemUserReference);
    }

    @Test
    void filterEvent() throws Exception
    {
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.filter.filterEvent(nonSystemEvent, randomUser, Collections.emptyList(), NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
            this.filter.filterEvent(systemEvent, randomUser, Collections.emptyList(), NotificationFormat.ALERT));
    }

    @Test
    void filterTargetedSystemEvent() throws Exception
    {
        when(this.systemEvent.getTarget()).thenReturn(new HashSet<>(Arrays.asList("user1", "user2")));

        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
            this.filter.filterEvent(systemEvent, randomUser, Collections.emptyList(), NotificationFormat.ALERT));
    }

    @Test
    void filterExpression() throws Exception
    {
        NotificationPreference fakePreference = mock(NotificationPreference.class);

        assertNull(this.filter.filterExpression(randomUser, Collections.emptyList(), fakePreference));
        assertEquals("USER <> \"serializedSystemUser\"", this.filter
            .filterExpression(randomUser, Collections.emptyList(), NotificationFilterType.EXCLUSIVE, null).toString());
    }

    @Test
    void matchesPreference() throws Exception
    {
        assertFalse(this.filter.matchesPreference(mock(NotificationPreference.class)));
    }

    @Test
    void getName() throws Exception
    {
        assertEquals(SystemUserNotificationFilter.FILTER_NAME, this.filter.getName());
    }
}
