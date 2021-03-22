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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
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
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link LiveNotificationEmailEventFilter}.
 *
 * @version $Id$
 */
@ComponentTest
public class LiveNotificationEmailEventFilterTest
{
    @InjectMockComponents
    private LiveNotificationEmailEventFilter eventFilter;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    private NotificationPreferenceManager notificationPreferenceManager;

    @MockComponent
    private DocumentReferenceResolver<String> referenceResolver;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private UserPropertiesResolver userPropertiesResolver;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    private DocumentReference user = new DocumentReference("xwiki", "XWiki", "Foo");

    @Test
    void canAccessEventNoDocument() throws NotificationException
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        assertTrue(this.eventFilter.canAccessEvent(this.user, compositeEvent));
        verify(this.authorizationManager, never()).hasAccess(any(), any(), any());
    }

    @Test
    void canAccessEventDocumentNoRights() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        DocumentReference eventDoc = new DocumentReference("xwiki", "XWiki", "EventDoc");
        when(compositeEvent.getDocument()).thenReturn(eventDoc);
        when(this.authorizationManager.hasAccess(Right.VIEW, user, eventDoc)).thenReturn(false);
        assertFalse(this.eventFilter.canAccessEvent(user, compositeEvent));
        verify(this.documentAccessBridge, never()).getTranslatedDocumentInstance(eventDoc);
    }

    @Test
    void canAccessEventHiddenDocument() throws Exception
    {
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        DocumentReference eventDoc = new DocumentReference("xwiki", "XWiki", "EventDoc");
        when(compositeEvent.getDocument()).thenReturn(eventDoc);
        when(this.authorizationManager.hasAccess(Right.VIEW, user, eventDoc)).thenReturn(true);
        XWikiDocument xWikiDocument = mock(XWikiDocument.class);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(eventDoc)).thenReturn(xWikiDocument);
        when(xWikiDocument.isHidden()).thenReturn(false);
        assertTrue(this.eventFilter.canAccessEvent(this.user, compositeEvent));
        verify(this.userReferenceResolver, never()).resolve(this.user);

        when(xWikiDocument.isHidden()).thenReturn(true);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(this.user)).thenReturn(userReference);
        UserProperties userProperties = mock(UserProperties.class);
        when(this.userPropertiesResolver.resolve(userReference)).thenReturn(userProperties);
        when(userProperties.displayHiddenDocuments()).thenReturn(false);
        assertFalse(this.eventFilter.canAccessEvent(user, compositeEvent));

        when(userProperties.displayHiddenDocuments()).thenReturn(true);
        assertTrue(this.eventFilter.canAccessEvent(this.user, compositeEvent));
    }

    @Test
    void isEventFiltered() throws NotificationException
    {
        assertFalse(this.eventFilter.isEventFiltered(Collections.emptyList(), mock(Event.class), this.user));
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);

        Event event = mock(Event.class);
        Collection<NotificationFilterPreference> filterPreferences =
            Collections.singleton(mock(NotificationFilterPreference.class));
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.user)).thenReturn(filterPreferences);
        when(filter1.filterEvent(event, this.user, filterPreferences, NotificationFormat.EMAIL)).thenReturn(
            NotificationFilter.FilterPolicy.NO_EFFECT);
        when(filter2.filterEvent(event, this.user, filterPreferences, NotificationFormat.EMAIL)).thenReturn(
            NotificationFilter.FilterPolicy.KEEP);
        when(filter3.filterEvent(event, this.user, filterPreferences, NotificationFormat.EMAIL)).thenReturn(
            NotificationFilter.FilterPolicy.FILTER);

        assertFalse(this.eventFilter.isEventFiltered(Arrays.asList(filter1, filter2, filter3), event, this.user));

        when(filter1.filterEvent(event, this.user, filterPreferences, NotificationFormat.EMAIL)).thenReturn(
            NotificationFilter.FilterPolicy.NO_EFFECT);
        when(filter2.filterEvent(event, this.user, filterPreferences, NotificationFormat.EMAIL)).thenReturn(
            NotificationFilter.FilterPolicy.FILTER);
        when(filter3.filterEvent(event, this.user, filterPreferences, NotificationFormat.EMAIL)).thenReturn(
            NotificationFilter.FilterPolicy.KEEP);

        assertTrue(this.eventFilter.isEventFiltered(Arrays.asList(filter1, filter2, filter3), event, this.user));
        assertFalse(this.eventFilter.isEventFiltered(Collections.singletonList(filter1), event, this.user));
    }

    @Test
    void isCompositeEventHandledFromNotificationPreference() throws NotificationException
    {
        CompositeEvent event = mock(CompositeEvent.class);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.user))
            .thenReturn(Collections.emptyList());
        when(this.notificationPreferenceManager.getAllPreferences(this.user)).thenReturn(Collections.emptyList());
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        NotificationPreference pref1 = mock(NotificationPreference.class);
        NotificationPreference pref2 = mock(NotificationPreference.class);
        NotificationPreference pref3 = mock(NotificationPreference.class);
        List<NotificationPreference> allPrefs = Arrays.asList(pref1, pref2, pref3);
        when(this.notificationPreferenceManager.getAllPreferences(this.user)).thenReturn(allPrefs);
        when(pref1.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref2.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref3.getFormat()).thenReturn(NotificationFormat.ALERT);
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(pref2.getFormat()).thenReturn(NotificationFormat.EMAIL);
        when(pref3.getFormat()).thenReturn(NotificationFormat.EMAIL);
        when(pref2.getProperties()).thenReturn(Collections.emptyMap());
        when(pref3.getProperties()).thenReturn(Collections.emptyMap());
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        Map<NotificationPreferenceProperty, Object> propertyMap =
            Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "foo");
        when(event.getType()).thenReturn("bar");
        when(pref3.getProperties()).thenReturn(propertyMap);
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        propertyMap =
            Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "bar");
        when(pref3.getProperties()).thenReturn(propertyMap);
        when(pref3.isNotificationEnabled()).thenReturn(false);
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(pref3.isNotificationEnabled()).thenReturn(true);
        assertTrue(this.eventFilter.isCompositeEventHandled(this.user, event));
    }

    @Test
    void isCompositeEventHandledFollowedUser() throws NotificationException
    {
        CompositeEvent event = mock(CompositeEvent.class);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.user))
            .thenReturn(Collections.emptyList());
        when(this.notificationPreferenceManager.getAllPreferences(this.user)).thenReturn(Collections.emptyList());
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref3 = mock(NotificationFilterPreference.class);
        List<NotificationFilterPreference> allFilterPrefs = Arrays.asList(filterPref1, filterPref2, filterPref3);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.user)).thenReturn(allFilterPrefs);
        DocumentReference user2 = new DocumentReference("xwiki",  "XWiki", "Bar");
        DocumentReference user3 = new DocumentReference("xwiki",  "XWiki", "Toto");
        when(this.referenceResolver.resolve("xwiki:XWiki.Toto")).thenReturn(user3);
        when(this.referenceResolver.resolve("xwiki:XWiki.Bar")).thenReturn(user2);
        when(event.getUsers()).thenReturn(new HashSet<>(Arrays.asList(user2, user3)));

        when(filterPref2.isEnabled()).thenReturn(true);
        when(filterPref2.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(filterPref2.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(filterPref2.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        when(filterPref2.getEventTypes()).thenReturn(Collections.emptySet());
        when(filterPref2.getUser()).thenReturn("xwiki:XWiki.Toto");
        assertTrue(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref2.isEnabled()).thenReturn(false);
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref3.isEnabled()).thenReturn(true);
        when(filterPref3.getFilterName()).thenReturn("Something");
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref3.getFilterName()).thenReturn(EventUserFilter.FILTER_NAME);
        when(filterPref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(filterPref3.getFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref3.getFilterType()).thenReturn(NotificationFilterType.INCLUSIVE);
        when(filterPref3.getEventTypes()).thenReturn(Collections.singleton("something"));
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref3.getEventTypes()).thenReturn(Collections.emptySet());
        when(filterPref3.getUser()).thenReturn("xwiki:XWiki.Buz");
        assertFalse(this.eventFilter.isCompositeEventHandled(this.user, event));

        when(filterPref3.getUser()).thenReturn("xwiki:XWiki.Bar");
        assertTrue(this.eventFilter.isCompositeEventHandled(this.user, event));
    }
}
