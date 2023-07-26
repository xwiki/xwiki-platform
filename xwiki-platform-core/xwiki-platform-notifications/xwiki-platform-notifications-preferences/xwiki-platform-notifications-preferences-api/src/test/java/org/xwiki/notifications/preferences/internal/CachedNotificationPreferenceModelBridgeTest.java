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
package org.xwiki.notifications.preferences.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.internal.cache.UnboundedEntityCacheManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ComponentTest
class CachedNotificationPreferenceModelBridgeTest
{
    @InjectMockComponents
    private CachedNotificationPreferenceModelBridge preferenceModelBridge;

    @MockComponent
    private NotificationPreferenceModelBridge notificationPreferenceModelBridge;

    @MockComponent
    private UnboundedEntityCacheManager cacheManager;

    @MockComponent
    private EntityReferenceFactory referenceFactory;

    private Map<EntityReference, Object> preferenceCache;
    private Map<EntityReference, Object> eventGroupingStrategyCache;

    @BeforeEach
    void setup() throws InitializationException
    {
        this.preferenceCache = new HashMap<>();
        this.eventGroupingStrategyCache = new HashMap<>();
        when(this.cacheManager.createCache("NotificationsPreferences", true)).thenReturn(this.preferenceCache);
        when(this.cacheManager.createCache("EventGroupingNotificationsPreferences", true))
            .thenReturn(this.eventGroupingStrategyCache);
        this.preferenceModelBridge.initialize();
        when(this.referenceFactory.getReference(any())).then(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @Test
    void getNotificationsPreferences() throws NotificationException
    {
        assertEquals(Collections.emptyList(),
            this.preferenceModelBridge.getNotificationsPreferences((DocumentReference) null));

        DocumentReference documentReference = mock(DocumentReference.class);
        List<NotificationPreference> preferences = List.of(
            mock(NotificationPreference.class),
            mock(NotificationPreference.class)
        );
        this.preferenceCache.put(documentReference, preferences);
        assertEquals(preferences, this.preferenceModelBridge.getNotificationsPreferences(documentReference));
        verifyNoInteractions(this.notificationPreferenceModelBridge);

        this.preferenceCache.clear();
        when(this.notificationPreferenceModelBridge.getNotificationsPreferences(documentReference))
            .thenReturn(preferences);
        assertEquals(preferences, this.preferenceModelBridge.getNotificationsPreferences(documentReference));
        assertEquals(1, this.preferenceCache.size());
        assertEquals(preferences, this.preferenceCache.get(documentReference));
        verify(this.referenceFactory).getReference(documentReference);
    }

    @Test
    void getNotificationsPreferencesForWiki() throws NotificationException
    {
        assertEquals(Collections.emptyList(),
            this.preferenceModelBridge.getNotificationsPreferences((WikiReference) null));

        WikiReference wikiReference = mock(WikiReference.class);
        List<NotificationPreference> preferences = List.of(
            mock(NotificationPreference.class),
            mock(NotificationPreference.class)
        );
        this.preferenceCache.put(wikiReference, preferences);
        assertEquals(preferences, this.preferenceModelBridge.getNotificationsPreferences(wikiReference));
        verifyNoInteractions(this.notificationPreferenceModelBridge);

        this.preferenceCache.clear();
        when(this.notificationPreferenceModelBridge.getNotificationsPreferences(wikiReference))
            .thenReturn(preferences);
        assertEquals(preferences, this.preferenceModelBridge.getNotificationsPreferences(wikiReference));
        assertEquals(1, this.preferenceCache.size());
        assertEquals(preferences, this.preferenceCache.get(wikiReference));
        verify(this.referenceFactory).getReference(wikiReference);
    }

    @Test
    void getEventGroupingStrategyHint() throws NotificationException
    {
        DocumentReference documentReference = mock(DocumentReference.class);
        Map<String, String> hints = Map.of("alert", "hint1", "email", "hint2");
        this.eventGroupingStrategyCache.put(documentReference, hints);

        assertEquals("hint1", this.preferenceModelBridge.getEventGroupingStrategyHint(documentReference, "alert"));
        assertEquals("hint2", this.preferenceModelBridge.getEventGroupingStrategyHint(documentReference, "email"));
        verifyNoInteractions(this.notificationPreferenceModelBridge);

        this.eventGroupingStrategyCache.clear();
        when(this.notificationPreferenceModelBridge.getEventGroupingStrategyHint(documentReference, "alert"))
            .thenReturn("hint1");
        when(this.notificationPreferenceModelBridge.getEventGroupingStrategyHint(documentReference, "email"))
            .thenReturn("hint2");
        assertEquals("hint1", this.preferenceModelBridge.getEventGroupingStrategyHint(documentReference, "alert"));
        assertEquals("hint2", this.preferenceModelBridge.getEventGroupingStrategyHint(documentReference, "email"));

        assertEquals(hints, this.eventGroupingStrategyCache.get(documentReference));
    }
}