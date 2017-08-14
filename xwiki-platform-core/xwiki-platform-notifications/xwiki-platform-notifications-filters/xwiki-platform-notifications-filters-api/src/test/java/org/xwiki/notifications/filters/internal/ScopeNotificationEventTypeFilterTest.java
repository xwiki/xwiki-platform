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
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.expression.AndNode;
import org.xwiki.notifications.filters.expression.EqualsNode;
import org.xwiki.notifications.filters.expression.LikeNode;
import org.xwiki.notifications.filters.expression.PropertyValueNode;
import org.xwiki.notifications.filters.expression.StringValueNode;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
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
    public void filterExpressionCase1() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();
        when(serializer.serialize(SCOPE_INCLUSIVE_REFERENCE_1)).thenReturn("wiki1");

        NotificationPreference prop1 = mock(NotificationPreference.class);
        when(prop1.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(prop1.getProperties()).thenReturn(
                Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "event1"));

        AbstractNode test1 = mocker.getComponentUnderTest().filterExpression(
                new DocumentReference("xwiki", "XWiki", "User"), prop1);

        AbstractNode expectedResult1 = new EqualsNode(
                        new PropertyValueNode(NotificationFilterProperty.WIKI),
                        new StringValueNode("wiki1"));

        assertEquals(expectedResult1, test1);
    }

    @Test
    public void filterExpressionCase2() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();
        when(serializer.serialize(SCOPE_INCLUSIVE_REFERENCE_2)).thenReturn("space_2");

        NotificationPreference pref2 = mock(NotificationPreference.class);
        when(pref2.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref2.getProperties()).thenReturn(
                Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "event2"));

        AbstractNode test2 = mocker.getComponentUnderTest().filterExpression(
                new DocumentReference("xwiki", "XWiki", "User"), pref2);

        AbstractNode expectedResult2 = new AndNode(
                new EqualsNode(
                        new PropertyValueNode(NotificationFilterProperty.WIKI),
                        new StringValueNode("wiki2")),
                new LikeNode(
                        new PropertyValueNode(NotificationFilterProperty.SPACE),
                        new StringValueNode("space_2")));

        assertEquals(expectedResult2, test2);
    }

    @Test
    public void filterExpressionCase3() throws Exception
    {
        // Mocks
        createPreferenceScopeMocks();
        when(serializer.serialize(SCOPE_INCLUSIVE_REFERENCE_3)).thenReturn("space3.page3");

        NotificationPreference pref3 = mock(NotificationPreference.class);
        when(pref3.getFormat()).thenReturn(NotificationFormat.ALERT);
        when(pref3.getProperties()).thenReturn(
                Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "event3"));

        AbstractNode test3 = mocker.getComponentUnderTest().filterExpression(
                new DocumentReference("xwiki", "XWiki", "User"), pref3);

        AbstractNode expectedResult3 = new AndNode(
                new EqualsNode(
                        new PropertyValueNode(NotificationFilterProperty.WIKI),
                        new StringValueNode("wiki3")),
                new EqualsNode(
                        new PropertyValueNode(NotificationFilterProperty.PAGE),
                        new StringValueNode("space3.page3")));

        assertEquals(expectedResult3, test3);
    }

    private void createPreferenceScopeMocks() throws NotificationException
    {
        NotificationPreferenceFilterScope scope1 = mock(NotificationPreferenceFilterScope.class);
        when(scope1.getScopeReference()).thenReturn(
                SCOPE_INCLUSIVE_REFERENCE_1
        );
        when(scope1.getEventTypes()).thenReturn(Arrays.asList("event1"));

        NotificationPreferenceFilterScope scope2 = mock(NotificationPreferenceFilterScope.class);
        when(scope2.getScopeReference()).thenReturn(
                SCOPE_INCLUSIVE_REFERENCE_2
        );
        when(scope2.getEventTypes()).thenReturn(Arrays.asList("event2"));

        NotificationPreferenceFilterScope scope3 = mock(NotificationPreferenceFilterScope.class);
        when(scope3.getScopeReference()).thenReturn(
                SCOPE_INCLUSIVE_REFERENCE_3
        );
        when(scope3.getEventTypes()).thenReturn(Arrays.asList("event3"));

        NotificationPreferenceFilterScope exclusiveScope1 = mock(NotificationPreferenceFilterScope.class);
        when(exclusiveScope1.getScopeReference()).thenReturn(SCOPE_EXCLUSIVE_REFERENCE_1);
        when(exclusiveScope1.getScopeFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(exclusiveScope1.getEventTypes()).thenReturn(Arrays.asList("exclusiveEvent1"));

        NotificationPreferenceFilterScope exclusiveScope2 = mock(NotificationPreferenceFilterScope.class);
        when(exclusiveScope2.getScopeReference()).thenReturn(SCOPE_EXCLUSIVE_REFERENCE_2);
        when(exclusiveScope2.getScopeFilterType()).thenReturn(NotificationFilterType.EXCLUSIVE);
        when(exclusiveScope2.getEventTypes()).thenReturn(Arrays.asList("exclusiveEvent2"));

        when(modelBridge.getNotificationPreferenceScopes(any(DocumentReference.class),
                any(NotificationFormat.class), eq(NotificationFilterType.INCLUSIVE))).thenReturn(
                        Arrays.asList(scope1, scope2, scope3)
        );

        when(modelBridge.getNotificationPreferenceScopes(any(DocumentReference.class),
                any(NotificationFormat.class), eq(NotificationFilterType.EXCLUSIVE))).thenReturn(
                        Arrays.asList(exclusiveScope1, exclusiveScope2)
        );
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
        when(preference.getCategory()).thenReturn(NotificationPreferenceCategory.SYSTEM);

        assertFalse(mocker.getComponentUnderTest().matchesPreference(preference));
    }

    @Test
    public void getName() throws Exception
    {
        assertEquals(ScopeNotificationEventTypeFilter.FILTER_NAME, mocker.getComponentUnderTest().getName());
    }
}
