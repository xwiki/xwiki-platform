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
package org.xwiki.notifications.notifiers.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.eventstream.Event;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link UserEventManager}.
 * 
 * @version $Id$
 */
@ComponentTest
public class UserEventManagerTest
{
    @InjectMockComponents
    private UserEventManager userEventManager;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private Logger logger;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private EntityReferenceFactory entityReferenceFactory;

    @BeforeEach
    void beforeEach()
    {
        when(this.entityReferenceFactory.getReference(any())).then(invocationOnMock -> invocationOnMock.getArgument(0));
    }

    @Test
    void isListeningNotificationPreference() throws Exception
    {
        Event event = mock(Event.class);
        DocumentReference userReference = new DocumentReference("xwiki", "User", "Foo");
        NotificationFormat format = NotificationFormat.ALERT;
        when(this.entityReferenceSerializer.serialize(userReference)).thenReturn("xwiki:User.Foo");

        // the user can see the the document referenced in the event
        DocumentReference eventDocumentReference = new DocumentReference("xwiki", "Foo", "Doc");
        when(event.getDocument()).thenReturn(eventDocumentReference);
        when(this.authorizationManager.hasAccess(Right.VIEW, userReference, eventDocumentReference)).thenReturn(true);

        when(event.getDate()).thenReturn(new Date(42));

        // the user has been created before the event was sent
        DocumentModelBridge userDoc = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getDocumentInstance(userReference)).thenReturn(userDoc);
        when(userDoc.getCreationDate()).thenReturn(new Date(40));

        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);

        // Specify 2 preferences just to check if we iterate properly
        List<NotificationPreference> preferenceList = Arrays.asList(pref1, pref2);
        when(this.notificationPreferenceManager.getAllPreferences(userReference)).thenReturn(preferenceList);

        // pref1 will be discarded immediately
        when(pref1.getFormat()).thenReturn(NotificationFormat.EMAIL);
        when(pref2.getFormat()).thenReturn(NotificationFormat.ALERT);

        // the preference is about the same event type
        when(event.getType()).thenReturn("mention");
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "mention"));

        // it started before the event was sent
        when(pref2.getStartDate()).thenReturn(new Date(41));

        // and the preference is enabled
        when(pref2.isNotificationEnabled()).thenReturn(true);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // if the preference is disabled it's not listened anymore
        when(pref2.isNotificationEnabled()).thenReturn(false);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // if the preference is enabled but after the event is sent, it's not listened
        when(pref2.getStartDate()).thenReturn(new Date(43));
        when(pref2.isNotificationEnabled()).thenReturn(true);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // if the preference is enabled but at the same date as the event is sent, it's listened
        when(pref2.getStartDate()).thenReturn(new Date(42));
        when(pref2.isNotificationEnabled()).thenReturn(true);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // if the start date is null we don't have enough information so we consider it's listened
        when(pref2.getStartDate()).thenReturn(null);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // date is now good but the event type is not the right one.
        when(pref2.getStartDate()).thenReturn(new Date(41));
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "something"));
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // event type is now good but the format is wrong
        when(pref2.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "mention"));
        when(pref2.getFormat()).thenReturn(NotificationFormat.EMAIL);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // format is now good but the user doc has been created after the event
        when(pref2.getFormat()).thenReturn(NotificationFormat.ALERT);
        DocumentReference userReference2 = new DocumentReference("xwiki", "User", "Foo2");
        DocumentModelBridge userDoc2 = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getDocumentInstance(userReference2)).thenReturn(userDoc2);
        when(userDoc2.getCreationDate()).thenReturn(new Date(43));
        when(this.authorizationManager.hasAccess(Right.VIEW, userReference2, eventDocumentReference)).thenReturn(true);
        when(this.notificationPreferenceManager.getAllPreferences(userReference2)).thenReturn(preferenceList);
        assertFalse(this.userEventManager.isListening(event, userReference2, format));

        // if the user creation date is null then we don't have enough information so we consider it's after the event
        DocumentReference userReference3 = new DocumentReference("xwiki", "User", "Foo3");
        DocumentModelBridge userDoc3 = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getDocumentInstance(userReference3)).thenReturn(userDoc3);
        when(this.authorizationManager.hasAccess(Right.VIEW, userReference3, eventDocumentReference)).thenReturn(true);
        when(this.notificationPreferenceManager.getAllPreferences(userReference3)).thenReturn(preferenceList);
        when(userDoc3.getCreationDate()).thenReturn(null);
        assertTrue(this.userEventManager.isListening(event, userReference3, format));

        // in the same way, if it's actually the event date which is null, we consider user creation date and
        // notification preference start date were before.
        when(event.getDate()).thenReturn(null);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // date is set back, but now the document is not authorized anymore
        when(event.getDate()).thenReturn(new Date(42));
        when(this.authorizationManager.hasAccess(Right.VIEW, userReference, eventDocumentReference)).thenReturn(false);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // if event's document is null we fallback on space.
        when(event.getDocument()).thenReturn(null);
        when(event.getSpace()).thenReturn(eventDocumentReference.getLastSpaceReference());
        when(this.authorizationManager
            .hasAccess(Right.VIEW, userReference, eventDocumentReference.getLastSpaceReference())).thenReturn(true);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        when(this.authorizationManager
            .hasAccess(Right.VIEW, userReference, eventDocumentReference.getLastSpaceReference())).thenReturn(false);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // if event's document and space are both null, we fallback on wiki.
        when(event.getSpace()).thenReturn(null);
        when(event.getWiki()).thenReturn(eventDocumentReference.getWikiReference());
        when(this.authorizationManager
            .hasAccess(Right.VIEW, userReference, eventDocumentReference.getWikiReference())).thenReturn(true);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        when(this.authorizationManager
            .hasAccess(Right.VIEW, userReference, eventDocumentReference.getWikiReference())).thenReturn(false);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // ensure the cache is properly handled
        verify(this.entityReferenceFactory).getReference(userReference);
        verify(this.documentAccessBridge).getDocumentInstance(userReference);
    }

    @Test
    void isListeningFollowedUser() throws Exception
    {
        Event event = mock(Event.class);
        DocumentReference userReference = new DocumentReference("xwiki", "User", "Foo");
        NotificationFormat format = NotificationFormat.ALERT;
        when(this.entityReferenceSerializer.serialize(userReference)).thenReturn("xwiki:User.Foo");

        DocumentReference followedUser = new DocumentReference("xwiki", "User", "Bar");
        String serializedFollowedUser = "xwiki:User.Bar";
        when(this.referenceResolver.resolve(serializedFollowedUser)).thenReturn(followedUser);

        // the user can see the the document referenced in the event
        DocumentReference eventDocumentReference = new DocumentReference("xwiki", "Foo", "Doc");
        when(event.getDocument()).thenReturn(eventDocumentReference);
        when(this.authorizationManager.hasAccess(Right.VIEW, userReference, eventDocumentReference)).thenReturn(true);

        when(event.getDate()).thenReturn(new Date(42));

        // the user has been created before the event was sent
        DocumentModelBridge userDoc = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getDocumentInstance(userReference)).thenReturn(userDoc);
        when(userDoc.getCreationDate()).thenReturn(new Date(40));

        // We want to only check if the event is triggered by a followed user, so we must ignore conditions when
        // the event is triggered by a matching preference, which is the case if there's no preference.
        // So we create a stub one.
        NotificationPreference notificationPreference = mock(NotificationPreference.class);
        when(this.notificationPreferenceManager.getAllPreferences(userReference))
            .thenReturn(Collections.singletonList(notificationPreference));

        // 2 filters to verify it iterates
        NotificationFilterPreference filter1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filter2 = mock(NotificationFilterPreference.class);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(userReference))
            .thenReturn(Arrays.asList(filter1, filter2));

        // filter1 is discarded immediately
        when(filter1.getStartingDate()).thenReturn(new Date(500));
        when(filter2.getStartingDate()).thenReturn(new Date(41));

        // filter2 is enabled and is an inclusive user event filter matching formats, event types
        // and dedicated to filter the followed user.
        when(filter2.isEnabled()).thenReturn(true);
        when(filter2.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(filter2.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.EMAIL, NotificationFormat.ALERT)));
        when(filter2.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        when(filter2.getEventTypes()).thenReturn(Collections.emptySet());
        when(filter2.getUser()).thenReturn(serializedFollowedUser);
        when(event.getUser()).thenReturn(followedUser);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // if event is not by the followed user we don't listen it
        when(event.getUser()).thenReturn(new DocumentReference("xwiki", "Someone", "Else"));
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // same if the event user is null.
        when(event.getUser()).thenReturn(null);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // on the contrary if the filter is not for the followed user, we don't listen the event either.
        when(event.getUser()).thenReturn(followedUser);
        when(filter2.getUser()).thenReturn("myUser");
        when(this.referenceResolver.resolve("myUser")).thenReturn(new DocumentReference("xwiki", "Someone", "Else"));
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // the filter is about the right user, but the filter event types is wrong.
        when(filter2.getUser()).thenReturn(serializedFollowedUser);
        when(filter2.getEventTypes()).thenReturn(Collections.singleton("mentions"));
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // the filter event types is ok, but the filter is not inclusive.
        when(filter2.getEventTypes()).thenReturn(Collections.emptySet());
        when(filter2.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // the filter is inclusive but it only targets the wrong format
        when(filter2.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        when(filter2.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // the filter targets the right format, but it's not the right filter name.
        when(filter2.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        when(filter2.getFilterName()).thenReturn("myFilter");
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // filter name is OK but it's disabled
        when(filter2.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(filter2.isEnabled()).thenReturn(false);
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // filter is enabled but after the event is created
        when(filter2.isEnabled()).thenReturn(true);
        when(filter2.getStartingDate()).thenReturn(new Date(43));
        assertFalse(this.userEventManager.isListening(event, userReference, format));

        // however if the filter date is null we consider it has been enabled before the event
        when(filter2.getStartingDate()).thenReturn(null);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // if the filter date is the same date, then we keep the event too.
        when(filter2.getStartingDate()).thenReturn(new Date(42));
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // on the same way if the filter date is set, but the event date is null we consider the event
        // happens after the filter is enabled
        when(filter2.getStartingDate()).thenReturn(new Date(41));
        when(event.getDate()).thenReturn(null);
        assertTrue(this.userEventManager.isListening(event, userReference, format));

        // ensure the cache is properly handled
        verify(this.entityReferenceFactory).getReference(userReference);
        verify(this.documentAccessBridge).getDocumentInstance(userReference);
    }
}
