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

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemUserNotificationFilterTest
{
    @Rule
    public final MockitoComponentMockingRule<SystemUserNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(SystemUserNotificationFilter.class);

    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Event nonSystemEvent;
    private Event systemEvent;

    private DocumentReference randomUser = new DocumentReference("xwiki", "XWiki", "alice");

    @Before
    public void setUp() throws Exception
    {
        DocumentReference systemUserReference = new DocumentReference("xwiki", "XWiki", "superadmin");
        DocumentReference randomUserReference = new DocumentReference("xwiki", "XWiki", "bob");

        entityReferenceSerializer =
                mocker.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        when(entityReferenceSerializer.serialize(
                eq(systemUserReference.getLocalDocumentReference()))).thenReturn("serializedSystemUser");

        nonSystemEvent = mock(Event.class);
        when(nonSystemEvent.getUser()).thenReturn(randomUserReference);

        systemEvent = mock(Event.class);
        when(systemEvent.getUser()).thenReturn(systemUserReference);
    }

    @Test
    public void filterEvent() throws Exception
    {
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(nonSystemEvent, randomUser, Collections.emptyList(),
                NotificationFormat.ALERT));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(systemEvent, randomUser, Collections.emptyList(),
                NotificationFormat.ALERT));
    }

    @Test
    public void filterExpression() throws Exception
    {
        NotificationPreference fakePreference = mock(NotificationPreference.class);

        assertNull(mocker.getComponentUnderTest().filterExpression(randomUser, Collections.emptyList(), fakePreference));
        assertEquals("USER <> \"serializedSystemUser\"",
                mocker.getComponentUnderTest().filterExpression(randomUser, Collections.emptyList(),
                        NotificationFilterType.EXCLUSIVE,
                        null).toString());
    }

    @Test
    public void matchesPreference() throws Exception
    {
        assertFalse(mocker.getComponentUnderTest().matchesPreference(mock(NotificationPreference.class)));
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals(SystemUserNotificationFilter.FILTER_NAME, mocker.getComponentUnderTest().getName());
    }
}
