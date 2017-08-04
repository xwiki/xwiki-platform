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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.filters.internal.ModelBridge;
import org.xwiki.notifications.filters.internal.NotificationPreferenceFilterScope;
import org.xwiki.notifications.filters.internal.NotificationPreferenceScopeFilterType;
import org.xwiki.notifications.filters.internal.ScopeNotificationEventTypeFilter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class ScopeNotificationEventTypeFilterTest
{
    public static final WikiReference SCOPE_INCLUSIVE_REFERENCE_1 = new WikiReference("wiki1");

    public static final SpaceReference SCOPE_INCLUSIVE_REFERENCE_2 =
            new SpaceReference("space2", new WikiReference("wiki2"));

    public static final DocumentReference SCOPE_INCLUSIVE_REFERENCE_3 =
            new DocumentReference("wiki3", "space3", "page3");

    public static final SpaceReference SCOPE_EXCLUSIVE_REFERENCE_1 =
            new SpaceReference("excludedWiki", "space1");

    public static final DocumentReference SCOPE_EXCLUSIVE_REFERENCE_2 =
            new DocumentReference("excludedWiki", "space2", "page2");

    @Rule
    public final MockitoComponentMockingRule<ScopeNotificationEventTypeFilter> mocker =
            new MockitoComponentMockingRule<>(ScopeNotificationEventTypeFilter.class);

    private ModelBridge modelBridge;
    private EntityReferenceSerializer<String> serializer;

    @Before
    public void setUp() throws Exception
    {
        modelBridge = mocker.getInstance(ModelBridge.class, "cached");
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
    }

    @Test
    public void filterEvent() throws Exception
    {
        createPreferenceScopeMocks();

        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        Event event1 = mock(Event.class);
        when(event1.getType()).thenReturn("event1");
        when(event1.getDocument()).thenReturn(
                new DocumentReference("wiki1", "Main", "WebHome"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event1, user, NotificationFormat.ALERT));

        Event event2 = mock(Event.class);
        when(event2.getType()).thenReturn("event1");
        when(event2.getDocument()).thenReturn(
                new DocumentReference("someOtherWiki", "Main", "WebHome"));
        assertTrue(mocker.getComponentUnderTest().filterEvent(event2, user, NotificationFormat.ALERT));

        Event event3 = mock(Event.class);
        when(event3.getType()).thenReturn("event2");
        when(event3.getDocument()).thenReturn(
                new DocumentReference("wiki2", "space2", "WebHome"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event3, user, NotificationFormat.ALERT));

        Event event3bis = mock(Event.class);
        when(event3bis.getType()).thenReturn("event2");
        when(event3bis.getDocument()).thenReturn(
                new DocumentReference("wiki2", Arrays.asList("space2", "subspace"), "WebHome"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event3bis, user, NotificationFormat.ALERT));

        Event event4 = mock(Event.class);
        when(event4.getType()).thenReturn("event2");
        when(event4.getDocument()).thenReturn(
                new DocumentReference("wiki2", "otherSpace", "WebHome"));
        assertTrue(mocker.getComponentUnderTest().filterEvent(event4, user, NotificationFormat.ALERT));

        Event event5 = mock(Event.class);
        when(event5.getType()).thenReturn("event3");
        when(event5.getDocument()).thenReturn(
                new DocumentReference("wiki3", "space3", "page3"));
        assertFalse(mocker.getComponentUnderTest().filterEvent(event5, user, NotificationFormat.ALERT));

        Event event6 = mock(Event.class);
        when(event6.getType()).thenReturn("event3");
        when(event6.getDocument()).thenReturn(
                new DocumentReference("wiki3", "space3", "otherPage"));
        assertTrue(mocker.getComponentUnderTest().filterEvent(event6, user, NotificationFormat.ALERT));

        Event event7 = mock(Event.class);
        when(event7.getType()).thenReturn("eventWeDontCare");
        assertFalse(mocker.getComponentUnderTest().filterEvent(event7, user, NotificationFormat.ALERT));
    }

    @Test
    public void queryFilterOR() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();

        // Verify
        assertEquals(
                "((event.type = :type_scopeNotifEventTypeFilter_INCLUSIVE_1) AND "
                        + "event.wiki = :wiki_scopeNotifEventTypeFilter_INCLUSIVE_1)",
                mocker.getComponentUnderTest().queryFilterOR(
                    new DocumentReference("xwiki", "XWiki", "User"),
                    NotificationFormat.ALERT, Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "event1")
        ));

        assertEquals(
                "((event.type = :type_scopeNotifEventTypeFilter_INCLUSIVE_2) AND "
                        + "event.wiki = :wiki_scopeNotifEventTypeFilter_INCLUSIVE_2 " +
                                "AND event.space LIKE :space_scopeNotifEventTypeFilter_INCLUSIVE_2 ESCAPE '!')",
                mocker.getComponentUnderTest().queryFilterOR(
                    new DocumentReference("xwiki", "XWiki", "User"),
                    NotificationFormat.ALERT, Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "event2")
        ));

        assertEquals(
                "((event.type = :type_scopeNotifEventTypeFilter_INCLUSIVE_3) AND "
                        + "event.wiki = :wiki_scopeNotifEventTypeFilter_INCLUSIVE_3 "
                        + "AND event.page = :page_scopeNotifEventTypeFilter_INCLUSIVE_3)",
                mocker.getComponentUnderTest().queryFilterOR(
                    new DocumentReference("xwiki", "XWiki", "User"),
                    NotificationFormat.ALERT, Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "event3")
        ));
    }

    private void createPreferenceScopeMocks() throws NotificationException
    {
        NotificationPreferenceFilterScope scope1 = mock(NotificationPreferenceFilterScope.class);
        when(scope1.getScopeReference()).thenReturn(
                SCOPE_INCLUSIVE_REFERENCE_1
        );
        when(scope1.getEventType()).thenReturn("event1");

        NotificationPreferenceFilterScope scope2 = mock(NotificationPreferenceFilterScope.class);
        when(scope2.getScopeReference()).thenReturn(
                SCOPE_INCLUSIVE_REFERENCE_2
        );
        when(scope2.getEventType()).thenReturn("event2");

        NotificationPreferenceFilterScope scope3 = mock(NotificationPreferenceFilterScope.class);
        when(scope3.getScopeReference()).thenReturn(
                SCOPE_INCLUSIVE_REFERENCE_3
        );
        when(scope3.getEventType()).thenReturn("event3");

        NotificationPreferenceFilterScope exclusiveScope1 = mock(NotificationPreferenceFilterScope.class);
        when(exclusiveScope1.getScopeReference()).thenReturn(SCOPE_EXCLUSIVE_REFERENCE_1);
        when(exclusiveScope1.getScopeFilterType()).thenReturn(NotificationPreferenceScopeFilterType.EXCLUSIVE);
        when(exclusiveScope1.getEventType()).thenReturn("exclusiveEvent1");

        NotificationPreferenceFilterScope exclusiveScope2 = mock(NotificationPreferenceFilterScope.class);
        when(exclusiveScope2.getScopeReference()).thenReturn(SCOPE_EXCLUSIVE_REFERENCE_2);
        when(exclusiveScope2.getScopeFilterType()).thenReturn(NotificationPreferenceScopeFilterType.EXCLUSIVE);
        when(exclusiveScope2.getEventType()).thenReturn("exclusiveEvent2");

        when(modelBridge.getNotificationPreferenceScopes(any(DocumentReference.class),
                any(NotificationFormat.class), eq(NotificationPreferenceScopeFilterType.INCLUSIVE))).thenReturn(
                        Arrays.asList(scope1, scope2, scope3)
        );

        when(modelBridge.getNotificationPreferenceScopes(any(DocumentReference.class),
                any(NotificationFormat.class), eq(NotificationPreferenceScopeFilterType.EXCLUSIVE))).thenReturn(
                        Arrays.asList(exclusiveScope1, exclusiveScope2)
        );
    }

    @Test
    public void queryFilterAND() throws Exception
    {
        createPreferenceScopeMocks();

        assertEquals(
                StringUtils.EMPTY,
                mocker.getComponentUnderTest().queryFilterAND(
                        new DocumentReference("xwiki", "XWiki", "User"),
                        NotificationFormat.ALERT,
                        Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "type1")
                )
        );

        assertEquals(
                " NOT ((event.type = :type_scopeNotifEventTypeFilter_EXCLUSIVE_1) AND "
                        + "event.wiki = :wiki_scopeNotifEventTypeFilter_EXCLUSIVE_1 "
                        + "AND event.space LIKE :space_scopeNotifEventTypeFilter_EXCLUSIVE_1 ESCAPE '!')",
                mocker.getComponentUnderTest().queryFilterAND(
                        new DocumentReference("xwiki", "XWiki", "User"),
                        NotificationFormat.ALERT,
                        Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "exclusiveEvent1")
                )
        );

        assertEquals(
                " NOT ((event.type = :type_scopeNotifEventTypeFilter_EXCLUSIVE_1) AND "
                        + "event.wiki = :wiki_scopeNotifEventTypeFilter_EXCLUSIVE_1 "
                        + "AND event.space LIKE :space_scopeNotifEventTypeFilter_EXCLUSIVE_1 ESCAPE '!')",
                mocker.getComponentUnderTest().queryFilterAND(
                        new DocumentReference("xwiki", "XWiki", "User"),
                        NotificationFormat.ALERT,
                        Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "exclusiveEvent1")
                )
        );
    }

    @Test
    public void queryFilterParams() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();
        when(serializer.serialize(SCOPE_INCLUSIVE_REFERENCE_1)).thenReturn("wiki1");
        when(serializer.serialize(SCOPE_INCLUSIVE_REFERENCE_2)).thenReturn("space_2");
        when(serializer.serialize(SCOPE_INCLUSIVE_REFERENCE_3)).thenReturn("space3.page3");
        when(serializer.serialize(SCOPE_EXCLUSIVE_REFERENCE_1)).thenReturn("space1");
        when(serializer.serialize(SCOPE_EXCLUSIVE_REFERENCE_2)).thenReturn("space2.page2");

        // Prepare the notifications properties
        List<Map<NotificationPreferenceProperty, Object>> propertiesList = new ArrayList<>();
        for (String element : Arrays.asList("event1", "event2", "event3",
                "exclusiveEvent1", "exclusiveEvent2")) {
            propertiesList.add(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, element));
        }

        // Test
        Map<String, Object> results = mocker.getComponentUnderTest().queryFilterParams(
                new DocumentReference("xwiki", "XWiki", "User"),
                NotificationFormat.ALERT, propertiesList
        );

        // Verify
        assertEquals("wiki1", results.get("wiki_scopeNotifEventTypeFilter_INCLUSIVE_1"));
        assertEquals("wiki2", results.get("wiki_scopeNotifEventTypeFilter_INCLUSIVE_2"));
        assertEquals("space!_2%", results.get("space_scopeNotifEventTypeFilter_INCLUSIVE_2"));
        assertEquals("wiki3", results.get("wiki_scopeNotifEventTypeFilter_INCLUSIVE_3"));
        assertEquals("space3.page3", results.get("page_scopeNotifEventTypeFilter_INCLUSIVE_3"));
        assertEquals("excludedWiki", results.get("wiki_scopeNotifEventTypeFilter_EXCLUSIVE_1"));
        assertEquals("space1%", results.get("space_scopeNotifEventTypeFilter_EXCLUSIVE_1"));
        assertEquals("excludedWiki", results.get("wiki_scopeNotifEventTypeFilter_EXCLUSIVE_2"));
        assertEquals("space2.page2", results.get("page_scopeNotifEventTypeFilter_EXCLUSIVE_2"));
        assertEquals(9, results.size());
    }

    @Test
    public void matchPreferenceWithCorrectPreference() throws Exception
    {
        NotificationPreference preference = mock(NotificationPreference.class);
        when(preference.getCategory()).thenReturn(NotificationPreferenceCategory.DEFAULT);
        when(preference.getProperties()).thenReturn(
                Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, ""));

        assertTrue(mocker.getComponentUnderTest().matchesPreference(preference));
    }

    @Test
    public void matchPreferenceWithInorrectPreference() throws Exception
    {
        NotificationPreference preference = mock(NotificationPreference.class);
        when(preference.getCategory()).thenReturn(NotificationPreferenceCategory.WATCHLIST);

        assertFalse(mocker.getComponentUnderTest().matchesPreference(preference));
    }
}
