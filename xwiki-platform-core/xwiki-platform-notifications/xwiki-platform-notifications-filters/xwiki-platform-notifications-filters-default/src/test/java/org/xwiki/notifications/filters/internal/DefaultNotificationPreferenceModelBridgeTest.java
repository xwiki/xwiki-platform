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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultModelBridge}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultNotificationPreferenceModelBridgeTest
{
    @InjectMockComponents
    private DefaultModelBridge defaultModelBridge;

    @MockComponent
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    private final DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");
    private final WikiReference wikiReference = new WikiReference("foo");

    @Test
    void saveFilterPreferencesForUser() throws NotificationException
    {
        List<NotificationFilterPreference> filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class),
            mock(NotificationFilterPreference.class)
        );
        this.defaultModelBridge.saveFilterPreferences(user, filterPreferenceList);
        verify(this.notificationFilterPreferenceStore).saveFilterPreferences(user, filterPreferenceList);
    }

    @Test
    void saveFilterPreferencesForWiki() throws NotificationException
    {
        List<NotificationFilterPreference> filterPreferenceList = Arrays.asList(
            mock(NotificationFilterPreference.class),
            mock(NotificationFilterPreference.class)
        );
        this.defaultModelBridge.saveFilterPreferences(wikiReference, filterPreferenceList);
        verify(this.notificationFilterPreferenceStore).saveFilterPreferences(wikiReference, filterPreferenceList);
    }

    @Test
    void setFilterPreferenceEnabledForUser() throws NotificationException
    {
        String filterPrefName = "filter1";
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        when(this.notificationFilterPreferenceStore.getFilterPreference(this.user, filterPrefName))
            .thenReturn(filterPreference);
        when(filterPreference.isEnabled()).thenReturn(true);

        // the filter does not exist: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.user, "filter2", false);
        verify(this.notificationFilterPreferenceStore).getFilterPreference(this.user, "filter2");
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(DocumentReference.class), any());

        // the filter exists but is already set to false: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.user, filterPrefName, true);
        verify(filterPreference).isEnabled();
        verify(filterPreference, never()).setEnabled(true);
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(DocumentReference.class), any());

        this.defaultModelBridge.setFilterPreferenceEnabled(this.user, filterPrefName, false);
        verify(filterPreference).setEnabled(false);
        verify(this.notificationFilterPreferenceStore)
            .saveFilterPreferences(this.user, Collections.singletonList(filterPreference));
    }

    @Test
    void setFilterPreferenceEnabledForWiki() throws NotificationException
    {
        String filterPrefName = "filterA";
        NotificationFilterPreference filterPreference = mock(NotificationFilterPreference.class);
        when(this.notificationFilterPreferenceStore.getFilterPreference(this.wikiReference, filterPrefName))
            .thenReturn(filterPreference);
        when(filterPreference.isEnabled()).thenReturn(true);

        // the filter does not exist: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.wikiReference, "filter2", false);
        verify(this.notificationFilterPreferenceStore).getFilterPreference(this.wikiReference, "filter2");
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(WikiReference.class), any());

        // the filter exists but is already set to false: nothing happens
        this.defaultModelBridge.setFilterPreferenceEnabled(this.wikiReference, filterPrefName, true);
        verify(filterPreference).isEnabled();
        verify(filterPreference, never()).setEnabled(true);
        verify(this.notificationFilterPreferenceStore, never())
            .saveFilterPreferences(any(WikiReference.class), any());

        this.defaultModelBridge.setFilterPreferenceEnabled(this.wikiReference, filterPrefName, false);
        verify(filterPreference).setEnabled(false);
        verify(this.notificationFilterPreferenceStore)
            .saveFilterPreferences(this.wikiReference, Collections.singletonList(filterPreference));
    }

    @Test
    void deleteFilterPreferencesWiki() throws Exception
    {
        this.defaultModelBridge.deleteFilterPreferences(this.wikiReference);
        verify(this.notificationFilterPreferenceStore).deleteFilterPreference(this.wikiReference);
    }

    @Test
    void deleteFilterPreferencesUser() throws Exception
    {
        DocumentReference deletedUserDocumentReference = new DocumentReference("xwiki", "wXWiki", "DeletedUser");
        this.defaultModelBridge.deleteFilterPreferences(deletedUserDocumentReference);
        verify(this.notificationFilterPreferenceStore).deleteFilterPreferences(deletedUserDocumentReference);
    }
}
