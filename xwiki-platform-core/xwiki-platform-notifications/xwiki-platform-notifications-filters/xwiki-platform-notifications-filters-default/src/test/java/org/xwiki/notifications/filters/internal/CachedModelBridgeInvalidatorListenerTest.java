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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceAddOrUpdatedEvent;
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceDeletedEvent;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CachedModelBridgeInvalidatorListener}.
 *
 * @version $Id$
 */
@ComponentTest
class CachedModelBridgeInvalidatorListenerTest
{
    private static final DocumentReference USER = new DocumentReference("wiki", "XWiki", "User");

    private static final WikiReference WIKI = new WikiReference("wiki");

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private CachedModelBridgeInvalidatorListener listener;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    private CachedFilterPreferencesModelBridge cachedBridge;

    @BeforeComponent
    void beforeComponent() throws Exception
    {
        // The listener casts the "cached" bridge to its implementation.
        this.cachedBridge = mock(CachedFilterPreferencesModelBridge.class);
        this.componentManager.registerComponent(FilterPreferencesModelBridge.class, "cached", this.cachedBridge);
    }

    @Test
    void getEvents()
    {
        assertEquals(List.of(NotificationFilterPreferenceAddOrUpdatedEvent.class,
            NotificationFilterPreferenceDeletedEvent.class),
            this.listener.getEvents().stream().map(Object::getClass).toList());
    }

    @Test
    void onDeletedEventForUser()
    {
        // As NotificationFilterPreferenceStore#deleteFilterPreferences(DocumentReference, Set) notifies it.
        this.listener.onEvent(new NotificationFilterPreferenceDeletedEvent(), USER, Set.of("NFP_42"));

        verify(this.cachedBridge).invalidatePreferencefilter(USER);
    }

    @Test
    void onDeletedEventForWiki()
    {
        this.listener.onEvent(new NotificationFilterPreferenceDeletedEvent(), WIKI, "NFP_42");

        verify(this.cachedBridge).invalidatePreferencefilter(WIKI);
    }

    @Test
    void onDeletedEventReceivedFromAnotherMember() throws Exception
    {
        // The event, its source and its data are what the remote observation serializes for the other members.
        Object[] received = (Object[]) deserialize(serialize(
            new Object[] { new NotificationFilterPreferenceDeletedEvent(), USER, new HashSet<>(Set.of("NFP_42")) }));

        this.listener.onEvent((NotificationFilterPreferenceDeletedEvent) received[0], received[1], received[2]);

        verify(this.cachedBridge).invalidatePreferencefilter(USER);
    }

    @Test
    void onAddOrUpdatedEventForUser()
    {
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();
        preference.setOwner("wiki:XWiki.User");
        when(this.resolver.resolve("wiki:XWiki.User")).thenReturn(USER);

        this.listener.onEvent(new NotificationFilterPreferenceAddOrUpdatedEvent(), preference, null);

        verify(this.cachedBridge).invalidatePreferencefilter(USER);
    }

    @Test
    void onAddOrUpdatedEventForWiki()
    {
        DefaultNotificationFilterPreference preference = new DefaultNotificationFilterPreference();
        preference.setOwner("wiki");

        this.listener.onEvent(new NotificationFilterPreferenceAddOrUpdatedEvent(), preference, null);

        verify(this.cachedBridge).invalidatePreferencefilter(WIKI);
    }

    @Test
    void onEventWithoutOwner()
    {
        this.listener.onEvent(new NotificationFilterPreferenceAddOrUpdatedEvent(),
            new DefaultNotificationFilterPreference(), null);
        this.listener.onEvent(new NotificationFilterPreferenceDeletedEvent(), null, null);

        verifyNoInteractions(this.cachedBridge);
    }

    private static byte[] serialize(Object object) throws Exception
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(object);
        }

        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes) throws Exception
    {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return input.readObject();
        }
    }
}
