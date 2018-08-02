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
package org.xwiki.notifications.filters.internal.scope;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.internal.util.collections.Sets;
import org.xwiki.eventstream.Event;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.LocationOperatorNodeGenerator;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentList({
    LocationOperatorNodeGenerator.class,
    ScopeNotificationFilterExpressionGenerator.class,
    ScopeNotificationFilterPreferencesGetter.class,
    ScopeNotificationFilterLocationStateComputer.class
})
public class ScopeNotificationFilterTest
{
    @Rule
    public final MockitoComponentMockingRule<ScopeNotificationFilter> mocker =
            new MockitoComponentMockingRule<>(ScopeNotificationFilter.class);

    private NotificationFilterManager notificationFilterManager;
    private EntityReferenceSerializer<String> serializer;
    private EntityReferenceResolver<String> resolver;

    @Before
    public void setUp() throws Exception
    {
        notificationFilterManager = mock(NotificationFilterManager.class);
        mocker.registerComponent(NotificationFilterManager.class, notificationFilterManager);
        serializer = mock(EntityReferenceSerializer.class);
        mocker.registerComponent(EntityReferenceSerializer.TYPE_STRING, "local", serializer);
        resolver = mock(EntityReferenceResolver.class);
        mocker.registerComponent(EntityReferenceResolver.TYPE_STRING, resolver);
    }

    private NotificationFilterPreference mockNotificationFilterPreference(String entityStringValue,
            EntityReference resultReference, NotificationFilterType filterType, String eventName)
    {
        NotificationFilterProperty property;
        if (resultReference.getType().equals(EntityType.DOCUMENT)) {
            property = NotificationFilterProperty.PAGE;
        } else if (resultReference.getType().equals(EntityType.SPACE)) {
            property = NotificationFilterProperty.SPACE;
        } else {
            property = NotificationFilterProperty.WIKI;
        }

        NotificationFilterPreference preference = mock(NotificationFilterPreference.class);
        when(preference.getProperties(property)).thenReturn(Collections.singletonList(entityStringValue));
        when(preference.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(preference.getProperties(eq(NotificationFilterProperty.EVENT_TYPE))).thenReturn(
                eventName != null ? Arrays.asList(eventName) : Collections.emptyList());
        when(preference.getFilterType()).thenReturn(filterType);
        when(preference.getFilterFormats()).thenReturn(Sets.newSet(NotificationFormat.ALERT, NotificationFormat.EMAIL));
        when(preference.isEnabled()).thenReturn(true);

        when(resolver.resolve(entityStringValue, resultReference.getType())).thenReturn(resultReference);

        when(serializer.serialize(eq(resultReference))).thenReturn(entityStringValue);

        return preference;
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
    public void matchPreferenceWithIncorrectPreference() throws Exception
    {
        NotificationPreference preference = mock(NotificationPreference.class);
        when(preference.getCategory()).thenReturn(NotificationPreferenceCategory.SYSTEM);

        assertFalse(mocker.getComponentUnderTest().matchesPreference(preference));
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals(ScopeNotificationFilter.FILTER_NAME, mocker.getComponentUnderTest().getName());
    }

    @Test
    public void complexCase1() throws Exception
    {
        // Preferences:
        //
        // α: "update" event type enabled for format ALERT
        //
        // β: Exclusive filter on "wikiA".
        // γ: Inclusive filter on "wikiA:SpaceB"
        // δ: Exclusive filter on "wikiA:SpaceB.SpaceC"
        // ε: Exclusive filter on "wikiA:SpaceB.SpaceC.SpaceD"

        // Mock α
        NotificationPreference preference = mock(NotificationPreference.class);
        when(preference.getFormat()).thenReturn(NotificationFormat.ALERT);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(preference.getProperties()).thenReturn(properties);

        // Mock β
        WikiReference wikiReference = new WikiReference("wikiA");
        NotificationFilterPreference prefβ = mockNotificationFilterPreference("wikiA",
                wikiReference, NotificationFilterType.EXCLUSIVE, null);

        // Mock γ
        SpaceReference spaceReferenceB = new SpaceReference("SpaceB", wikiReference);
        NotificationFilterPreference prefγ = mockNotificationFilterPreference("wikiA:SpaceB",
                spaceReferenceB, NotificationFilterType.INCLUSIVE, "update");

        // Mock δ
        SpaceReference spaceReferenceC = new SpaceReference("SpaceC", spaceReferenceB);
        NotificationFilterPreference prefδ = mockNotificationFilterPreference("wikiA:SpaceB.SpaceC",
                spaceReferenceC, NotificationFilterType.EXCLUSIVE, null);

        // Mock ε
        SpaceReference spaceReferenceD = new SpaceReference("SpaceD", spaceReferenceC);
        NotificationFilterPreference prefε = mockNotificationFilterPreference("wikiA:SpaceB.SpaceC.SpaceD",
                spaceReferenceD, NotificationFilterType.INCLUSIVE, null);

        Collection<NotificationFilterPreference> filterPreferences = Sets.newSet(prefβ, prefγ, prefδ, prefε);

        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Test 1
        String result = mocker.getComponentUnderTest().filterExpression(user, filterPreferences, preference).toString();
        assertEquals("(((NOT (WIKI = \"wikiA\") OR (WIKI = \"wikiA\" AND SPACE STARTS WITH \"wikiA:SpaceB\"))" +
                " OR (WIKI = \"wikiA\" AND SPACE STARTS WITH \"wikiA:SpaceB.SpaceC.SpaceD\")) AND (NOT ((WIKI = \"wikiA\" " +
                "AND SPACE STARTS WITH \"wikiA:SpaceB.SpaceC\")) OR (WIKI = \"wikiA\" " +
                "AND SPACE STARTS WITH \"wikiA:SpaceB.SpaceC.SpaceD\")))", result);

        // Test with wikiA:SpaceE (filtered by β)
        Event event1 = mock(Event.class);
        when(event1.getSpace()).thenReturn(new SpaceReference("SpaceE", wikiReference));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event1, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiA:SpaceB.DocumentE (kept by γ)
        Event event2 = mock(Event.class);
        when(event2.getDocument()).thenReturn(new DocumentReference("DocumentE", spaceReferenceB));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event2, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiA:SpaceB.SpaceC.DocumentF (filtered by δ)
        Event event3 = mock(Event.class);
        when(event3.getDocument()).thenReturn(new DocumentReference("DocumentF", spaceReferenceC));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event3, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiA:SpaceB.SpaceC.SpaceD.DocumentG (kept by ε)
        Event event4 = mock(Event.class);
        when(event4.getDocument()).thenReturn(new DocumentReference("DocumentG", spaceReferenceD));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event4, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiB:SpaceH.DocumentI - kept because nothing match and there is no top level inclusive filter
        Event event5 = mock(Event.class);
        when(event5.getDocument()).thenReturn(new DocumentReference("wikiB", "SpaceH", "DocumentI"));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event5, user, filterPreferences, NotificationFormat.ALERT));
    }

    @Test
    public void testWithTopLevelInclusiveFilters() throws Exception
    {
        // Preferences:
        //
        // α: "update" event type enabled for format ALERT
        //
        // γ: Inclusive filter on "wikiA:SpaceB"
        // ζ: Inclusive filter on "wikiA:SpaceM.DocumentN"

        // Mock α
        NotificationPreference preference = mock(NotificationPreference.class);
        when(preference.getFormat()).thenReturn(NotificationFormat.ALERT);
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, "update");
        when(preference.getProperties()).thenReturn(properties);

        // Mock γ
        WikiReference wikiReference = new WikiReference("wikiA");
        SpaceReference spaceReferenceB = new SpaceReference("SpaceB", new WikiReference(wikiReference));
        NotificationFilterPreference prefγ = mockNotificationFilterPreference("wikiA:SpaceB",
                spaceReferenceB, NotificationFilterType.INCLUSIVE, null);

        // Mock ζ
        DocumentReference documentReference = new DocumentReference("wikiA", "SpaceM", "DocumentN");
        NotificationFilterPreference prefζ = mockNotificationFilterPreference("wikiA:SpaceM.DocumentN",
                documentReference, NotificationFilterType.INCLUSIVE, null);

        Collection<NotificationFilterPreference> filterPreferences = Sets.newSet(prefγ, prefζ);

        DocumentReference user = new DocumentReference("xwiki", "XWiki", "User");

        // Test 1
        String result = mocker.getComponentUnderTest().filterExpression(user, filterPreferences, preference).toString();
        assertEquals("((WIKI = \"wikiA\" AND SPACE STARTS WITH \"wikiA:SpaceB\") " +
                "OR (WIKI = \"wikiA\" AND PAGE = \"wikiA:SpaceM.DocumentN\"))", result);

        // Test with wikiA:SpaceE (filtered by γ & ζ)
        Event event1 = mock(Event.class);
        when(event1.getSpace()).thenReturn(new SpaceReference("SpaceE", wikiReference));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event1, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiA:SpaceB.DocumentJ (kept by γ)
        Event event2 = mock(Event.class);
        when(event2.getDocument()).thenReturn(new DocumentReference("wikiA", "SpaceB", "DocumentJ"));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event2, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiB:SpaceK.DocumentL (filtered by γ & ζ)
        Event event3 = mock(Event.class);
        when(event3.getDocument()).thenReturn(new DocumentReference("wikiB", "SpaceK", "DocumentL"));
        assertEquals(NotificationFilter.FilterPolicy.FILTER,
                mocker.getComponentUnderTest().filterEvent(event3, user, filterPreferences, NotificationFormat.ALERT));

        // Test with wikiA:SpaceM.DocumentN (kept by ζ)
        Event event4 = mock(Event.class);
        when(event4.getDocument()).thenReturn(new DocumentReference("wikiA", "SpaceM", "DocumentN"));
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT,
                mocker.getComponentUnderTest().filterEvent(event4, user, filterPreferences, NotificationFormat.ALERT));
    }
}
