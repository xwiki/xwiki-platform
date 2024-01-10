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
package org.xwiki.notifications.filters.script;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.internal.document.DocumentUserReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link NotificationFiltersScriptService}
 *
 * @version $Id$
 * @since 13.3RC1
 */
@ComponentTest
class NotificationFiltersScriptServiceTest
{
    @InjectMockComponents
    private NotificationFiltersScriptService scriptService;

    @MockComponent
    private NotificationFilterManager notificationFilterManager;

    @MockComponent
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @MockComponent
    @Named("cached")
    private FilterPreferencesModelBridge cachedFilterPreferencesModelBridge;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    private DocumentUserReference userReference;
    private DocumentReference userDocumentReference;
    private DocumentReference currentUserDocumentReference;
    private WikiReference wikiReference;

    @BeforeEach
    void setup()
    {
        this.userReference = mock(DocumentUserReference.class);
        this.userDocumentReference = mock(DocumentReference.class);
        this.currentUserDocumentReference = mock(DocumentReference.class);
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(this.currentUserDocumentReference);
        when(this.userReference.getReference()).thenReturn(this.userDocumentReference);
        this.wikiReference = new WikiReference("foo");
    }

    @Test
    void getToggleableNotificationFilters() throws NotificationException
    {
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);
        NotificationFilter filter4 = mock(NotificationFilter.class);
        NotificationFilter filter5 = mock(NotificationFilter.class);

        Collection<NotificationFilter> currentUserFilters = Arrays.asList(filter1, filter2, filter3);
        when(this.notificationFilterManager.getAllFilters(this.currentUserDocumentReference, false))
            .thenReturn(currentUserFilters);

        Collection<NotificationFilter> currentUserToggleableFilters = Arrays.asList(filter1, filter3);
        when(this.notificationFilterManager.getToggleableFilters(currentUserFilters))
            .thenReturn(currentUserToggleableFilters.stream());

        assertEquals(new HashSet<>(currentUserToggleableFilters),
            this.scriptService.getToggleableNotificationFilters());

        Collection<NotificationFilter> otherUserFilters = Arrays.asList(filter3, filter4, filter5);
        when(this.notificationFilterManager.getAllFilters(this.userDocumentReference, false))
            .thenReturn(otherUserFilters);

        Collection<NotificationFilter> otherUserToggleableFilters = Collections.singletonList(filter3);
        when(this.notificationFilterManager.getToggleableFilters(otherUserFilters))
            .thenReturn(otherUserToggleableFilters.stream());
        assertEquals(new HashSet<>(otherUserToggleableFilters),
            this.scriptService.getToggleableNotificationFilters(this.userReference));
    }

    @Test
    void getWikiToggleableNotificationFilters() throws NotificationException
    {
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);

        Collection<NotificationFilter> wikiFilters = Arrays.asList(filter1, filter2, filter3);
        when(this.notificationFilterManager.getAllFilters(this.wikiReference)).thenReturn(wikiFilters);

        Collection<NotificationFilter> wikiToggleableFilters = Arrays.asList(filter1, filter3);
        when(this.notificationFilterManager.getToggleableFilters(wikiFilters))
            .thenReturn(wikiToggleableFilters.stream());

        assertEquals(new HashSet<>(wikiToggleableFilters),
            this.scriptService.getWikiToggleableNotificationFilters(this.wikiReference));
    }

    @Test
    void getFilters() throws NotificationException
    {
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);
        NotificationFilter filter4 = mock(NotificationFilter.class);
        NotificationFilter filter5 = mock(NotificationFilter.class);

        Collection<NotificationFilter> currentUserFilters = Arrays.asList(filter1, filter2, filter3);
        when(this.notificationFilterManager.getAllFilters(this.currentUserDocumentReference, false))
            .thenReturn(currentUserFilters);


        assertEquals(currentUserFilters, this.scriptService.getFilters());

        Collection<NotificationFilter> otherUserFilters = Arrays.asList(filter3, filter4, filter5);
        when(this.notificationFilterManager.getAllFilters(this.userDocumentReference, false))
            .thenReturn(otherUserFilters);
        assertEquals(otherUserFilters, this.scriptService.getFilters(this.userReference));
    }

    @Test
    void getWikiFilters() throws NotificationException
    {
        NotificationFilter filter1 = mock(NotificationFilter.class);
        NotificationFilter filter2 = mock(NotificationFilter.class);
        NotificationFilter filter3 = mock(NotificationFilter.class);

        Collection<NotificationFilter> wikiFilters = Arrays.asList(filter1, filter2, filter3);
        when(this.notificationFilterManager.getAllFilters(this.wikiReference)).thenReturn(wikiFilters);

        assertEquals(wikiFilters, this.scriptService.getWikiFilters(this.wikiReference));
    }

    @Test
    void getFilterPreferences() throws NotificationException
    {
        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref3 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref4 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref5 = mock(NotificationFilterPreference.class);

        NotificationFilter filter = mock(NotificationFilter.class);

        Collection<NotificationFilterPreference> currentUserFiltersPref =
            Arrays.asList(filterPref1, filterPref2, filterPref3);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.currentUserDocumentReference))
            .thenReturn(currentUserFiltersPref);

        Collection<NotificationFilterPreference> filteredCurrentUserPref = Arrays.asList(filterPref1, filterPref3);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(currentUserFiltersPref, filter))
            .thenReturn(filteredCurrentUserPref.stream());

        assertEquals(new HashSet<>(filteredCurrentUserPref), this.scriptService.getFilterPreferences(filter));

        Collection<NotificationFilterPreference> otherUserFiltersPref =
            Arrays.asList(filterPref3, filterPref4, filterPref5);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.userDocumentReference))
            .thenReturn(otherUserFiltersPref);

        Collection<NotificationFilterPreference> filteredOtherUserPref = Collections.singletonList(filterPref3);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(otherUserFiltersPref, filter))
            .thenReturn(filteredOtherUserPref.stream());
        assertEquals(new HashSet<>(filteredOtherUserPref),
            this.scriptService.getFilterPreferences(filter, this.userReference));
    }

    @Test
    void getWikiFilterPreferences() throws NotificationException
    {
        NotificationFilterPreference filterPref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference filterPref3 = mock(NotificationFilterPreference.class);

        NotificationFilter filter = mock(NotificationFilter.class);

        Collection<NotificationFilterPreference> wikiFilterPrefs =
            Arrays.asList(filterPref1, filterPref2, filterPref3);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(this.wikiReference))
            .thenReturn(wikiFilterPrefs);

        Collection<NotificationFilterPreference> filteredWikiPrefs = Collections.singletonList(filterPref2);
        when(this.notificationFilterPreferenceManager.getFilterPreferences(wikiFilterPrefs, filter))
            .thenReturn(filteredWikiPrefs.stream());
        assertEquals(new HashSet<>(filteredWikiPrefs),
            this.scriptService.getWikiFilterPreferences(filter, this.wikiReference));
    }
}
