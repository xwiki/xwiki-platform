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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CachedFilterPreferencesModelBridge}.
 * 
 * @version $Id$
 */
@ComponentTest
class CachedFilterPreferencesModelBridgeTest
{
    private final static DocumentReference USER = new DocumentReference("wiki", "XWiki", "User");

    private final static String USER_STRING = "wiki:XWiki.User";

    private final static WikiReference WIKI = new WikiReference("wiki");

    @InjectMockComponents
    private CachedFilterPreferencesModelBridge cachedModelBridge;

    @MockComponent
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @InjectComponentManager
    private ComponentManager componentManager;

    private Map<EntityReference, Set<NotificationFilterPreference>> preferenceFilterCache;

    private Map<EntityReference, Map<String, Boolean>> toggleCache;

    @BeforeEach
    void beforeEach() throws IllegalAccessException
    {
        when(this.resolver.resolve(USER_STRING)).thenReturn(USER);

        this.preferenceFilterCache = (Map) FieldUtils.readField(this.cachedModelBridge, "preferenceFilterCache", true);
        this.toggleCache = (Map) FieldUtils.readField(this.cachedModelBridge, "toggleCache", true);
    }

    @Test
    void setStartDateForUser() throws Exception
    {
        Date date = new Date();
        this.cachedModelBridge.setStartDateForUser(USER, date);

        verify(this.filterPreferencesModelBridge).setStartDateForUser(USER, date);
    }

    @Test
    void setFilterPreferenceEnabledForUser() throws Exception
    {
        this.cachedModelBridge.setFilterPreferenceEnabled(USER, "filter1", true);

        verify(this.filterPreferencesModelBridge).setFilterPreferenceEnabled(USER, "filter1", true);
    }

    @Test
    void setFilterPreferenceEnabledForWiki() throws Exception
    {
        this.cachedModelBridge.setFilterPreferenceEnabled(WIKI, "filter2", false);

        verify(this.filterPreferencesModelBridge).setFilterPreferenceEnabled(WIKI, "filter2", false);
    }

    @Test
    void saveFilterPreferencesForUser() throws Exception
    {
        List<NotificationFilterPreference> filterPreferenceList =
            Arrays.asList(mock(NotificationFilterPreference.class), mock(NotificationFilterPreference.class));

        this.cachedModelBridge.saveFilterPreferences(USER, filterPreferenceList);

        verify(this.filterPreferencesModelBridge).saveFilterPreferences(USER, filterPreferenceList);
    }

    @Test
    void saveFilterPreferencesForWiki() throws Exception
    {
        List<NotificationFilterPreference> filterPreferenceList =
            Arrays.asList(mock(NotificationFilterPreference.class), mock(NotificationFilterPreference.class));

        this.cachedModelBridge.saveFilterPreferences(WIKI, filterPreferenceList);

        verify(this.filterPreferencesModelBridge).saveFilterPreferences(WIKI, filterPreferenceList);
    }

    @Test
    void invalidateUser()
    {
        this.preferenceFilterCache.put(USER, new HashSet<>());
        this.toggleCache.put(USER, new HashMap<>());

        this.cachedModelBridge.invalidatePreferencefilter(USER);

        assertNull(this.preferenceFilterCache.get(USER));
        assertNotNull(this.toggleCache.get(USER));
    }

    @Test
    void invalidateWiki()
    {
        this.preferenceFilterCache.put(USER, new HashSet<>());
        this.toggleCache.put(USER, new HashMap<>());
        this.preferenceFilterCache.put(WIKI, new HashSet<>());
        this.toggleCache.put(WIKI, new HashMap<>());

        this.cachedModelBridge.invalidatePreferencefilter(WIKI);

        assertNull(this.preferenceFilterCache.get(WIKI));
        assertNotNull(this.toggleCache.get(WIKI));
        assertNotNull(this.preferenceFilterCache.get(USER));
        assertNotNull(this.toggleCache.get(USER));
    }

    @Test
    void deleteFilterPreferencesWiki() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wikiId");

        NotificationFilterPreference notificationFilterPreference0 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference notificationFilterPreference1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference notificationFilterPreference2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference notificationFilterPreference3 = mock(NotificationFilterPreference.class);

        when(notificationFilterPreference0.isFromWiki("wikiId")).thenReturn(true);
        when(notificationFilterPreference1.isFromWiki("wikiId")).thenReturn(false);
        when(notificationFilterPreference2.isFromWiki("wikiId")).thenReturn(false);
        when(notificationFilterPreference3.isFromWiki("wikiId")).thenReturn(true);

        this.preferenceFilterCache.put(USER,
            new HashSet<>(Set.of(notificationFilterPreference0, notificationFilterPreference1)));
        this.preferenceFilterCache.put(WIKI,
            new HashSet<>(Set.of(notificationFilterPreference2, notificationFilterPreference3)));

        this.cachedModelBridge.deleteFilterPreferences(wikiReference);

        assertEquals(Map.of(
                USER, Set.of(notificationFilterPreference1),
                WIKI, Set.of(notificationFilterPreference2)),
            this.preferenceFilterCache);

        verify(this.filterPreferencesModelBridge).deleteFilterPreferences(wikiReference);
    }

    @Test
    void deleteFilterPreferencesUser() throws Exception
    {
        DocumentReference deleteUserDocumentReference = new DocumentReference("xwiki", "XWiki", "DeleteUser");
        Set<NotificationFilterPreference> otherUserSet = new HashSet<>();
        NotificationFilterPreference preference0 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference preference1 = mock(NotificationFilterPreference.class);
        otherUserSet.add(preference0);
        otherUserSet.add(preference1);

        when(preference0.getUser()).thenReturn("xwiki:XWiki.ExistingUser");
        when(preference1.getUser()).thenReturn("xwiki:XWiki.DeletedUser");
        when(this.entityReferenceSerializer.serialize(deleteUserDocumentReference))
            .thenReturn("xwiki:XWiki.DeletedUser");

        this.preferenceFilterCache.put(deleteUserDocumentReference, new HashSet<>());
        this.preferenceFilterCache.put(new DocumentReference("xwiki", "XWiki", "OtherUser"), otherUserSet);

        this.cachedModelBridge.deleteFilterPreferences(deleteUserDocumentReference);

        Map<Object, Object> expected = new HashMap<>();
        Set<Object> expectedOtherUserSet = new HashSet<>();
        expectedOtherUserSet.add(preference0);
        expected.put(new DocumentReference("xwiki", "XWiki", "OtherUser"), expectedOtherUserSet);
        assertEquals(expected, this.preferenceFilterCache);

        verify(this.filterPreferencesModelBridge).deleteFilterPreferences(deleteUserDocumentReference);
    }
}
