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
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ScopeNotificationFilterPreferencesGetter}.
 *
 * @version $Id$
 */
@ComponentTest
public class ScopeNotificationFilterPreferencesGetterTest
{
    @InjectMockComponents
    private ScopeNotificationFilterPreferencesGetter getter;

    @MockComponent
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Test
    void getScopeFilterPreferencesCriteriaAllCriteria()
    {
        NotificationFormat requestedFormat = NotificationFormat.ALERT;
        String requestedEventType = "mentions";
        boolean onlyGivenType = false;

        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref3 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref4 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref5 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref6 = mock(NotificationFilterPreference.class);

        // pref1 is not enabled, it will be discarded right away. Others are enabled.
        when(pref1.isEnabled()).thenReturn(false);
        when(pref2.isEnabled()).thenReturn(true);
        when(pref3.isEnabled()).thenReturn(true);
        when(pref4.isEnabled()).thenReturn(true);
        when(pref5.isEnabled()).thenReturn(true);
        when(pref6.isEnabled()).thenReturn(true);

        // pref2 has not the right filter name, it is discarded. Others have the right name.
        when(pref2.getFilterName()).thenReturn("Something");
        when(pref3.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref4.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref5.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref6.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);

        // pref3 has not the right matching format, it is discarded. Others at least contains it.
        when(pref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(pref4.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.ALERT, NotificationFormat.EMAIL)));
        when(pref5.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        when(pref6.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.ALERT, NotificationFormat.EMAIL)));

        // pref4 does not contain the right event type, it will be discarded.
        // pref5 does not contain any specific event type, so it should match all since onlyGivenType is false.
        when(pref4.getEventTypes()).thenReturn(Collections.singleton("otherEventType"));
        when(pref5.getEventTypes()).thenReturn(Collections.emptySet());
        when(pref6.getEventTypes()).thenReturn(new HashSet<>(Arrays.asList("like", "mentions", "comment")));

        ScopeNotificationFilterPreferencesHierarchy expected = new ScopeNotificationFilterPreferencesHierarchy(
            Arrays.asList(
                new ScopeNotificationFilterPreference(pref5, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref6, this.entityReferenceResolver)
        ));
        assertEquals(expected, this.getter.getScopeFilterPreferences(Arrays.asList(
            pref1, pref2, pref3, pref4, pref5, pref6
        ), requestedEventType, requestedFormat, onlyGivenType, false));
    }

    @Test
    void getScopeFilterPreferencesCriteriaFormatNull()
    {
        NotificationFormat requestedFormat = null;
        String requestedEventType = "mentions";
        boolean onlyGivenType = false;

        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref3 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref4 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref5 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref6 = mock(NotificationFilterPreference.class);

        // pref1 is not enabled, it will be discarded right away. Others are enabled.
        when(pref1.isEnabled()).thenReturn(false);
        when(pref2.isEnabled()).thenReturn(true);
        when(pref3.isEnabled()).thenReturn(true);
        when(pref4.isEnabled()).thenReturn(true);
        when(pref5.isEnabled()).thenReturn(true);
        when(pref6.isEnabled()).thenReturn(true);

        // pref2 has not the right filter name, it is discarded. Others have the right name.
        when(pref2.getFilterName()).thenReturn("Something");
        when(pref3.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref4.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref5.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref6.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);

        // All formats will be accepted
        when(pref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(pref4.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.ALERT, NotificationFormat.EMAIL)));
        when(pref5.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        when(pref6.getNotificationFormats()).thenReturn(null);

        // pref4 does not contain the right event type, it will be discarded.
        // pref5 does not contain any specific event type, so it should match all since onlyGivenType is false.
        when(pref3.getEventTypes()).thenReturn(Collections.singleton("mentions"));
        when(pref4.getEventTypes()).thenReturn(Collections.singleton("otherEventType"));
        when(pref5.getEventTypes()).thenReturn(Collections.emptySet());
        when(pref6.getEventTypes()).thenReturn(new HashSet<>(Arrays.asList("like", "mentions", "comment")));

        ScopeNotificationFilterPreferencesHierarchy expected = new ScopeNotificationFilterPreferencesHierarchy(
            Arrays.asList(
                new ScopeNotificationFilterPreference(pref3, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref5, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref6, this.entityReferenceResolver)
            ));
        assertEquals(expected, this.getter.getScopeFilterPreferences(Arrays.asList(
            pref1, pref2, pref3, pref4, pref5, pref6
        ), requestedEventType, requestedFormat, onlyGivenType, false));
    }

    @Test
    void getScopeFilterPreferencesCriteriaEventTypeNull()
    {
        NotificationFormat requestedFormat = null;
        String requestedEventType = null;
        boolean onlyGivenType = false;

        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref3 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref4 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref5 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref6 = mock(NotificationFilterPreference.class);

        // pref1 is not enabled, it will be discarded right away. Others are enabled.
        when(pref1.isEnabled()).thenReturn(false);
        when(pref2.isEnabled()).thenReturn(true);
        when(pref3.isEnabled()).thenReturn(true);
        when(pref4.isEnabled()).thenReturn(true);
        when(pref5.isEnabled()).thenReturn(true);
        when(pref6.isEnabled()).thenReturn(true);

        // pref2 has not the right filter name, it is discarded. Others have the right name.
        when(pref2.getFilterName()).thenReturn("Something");
        when(pref3.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref4.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref5.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref6.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);

        // All formats will be accepted
        when(pref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(pref4.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.ALERT, NotificationFormat.EMAIL)));
        when(pref5.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        when(pref6.getNotificationFormats()).thenReturn(null);

        // eventType is null and onlyGivenType is false so any event type will be accepted.
        when(pref3.getEventTypes()).thenReturn(Collections.singleton("mentions"));
        when(pref4.getEventTypes()).thenReturn(Collections.singleton("otherEventType"));
        when(pref5.getEventTypes()).thenReturn(Collections.emptySet());
        when(pref6.getEventTypes()).thenReturn(new HashSet<>(Arrays.asList("like", "mentions", "comment")));

        ScopeNotificationFilterPreferencesHierarchy expected = new ScopeNotificationFilterPreferencesHierarchy(
            Arrays.asList(
                new ScopeNotificationFilterPreference(pref3, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref4, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref5, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref6, this.entityReferenceResolver)
            ));
        assertEquals(expected, this.getter.getScopeFilterPreferences(Arrays.asList(
            pref1, pref2, pref3, pref4, pref5, pref6
        ), requestedEventType, requestedFormat, onlyGivenType, false));
    }

    @Test
    void getScopeFilterPreferencesCriteriaOnlyGivenType()
    {
        NotificationFormat requestedFormat = null;
        String requestedEventType = "mentions";
        boolean onlyGivenType = true;

        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref3 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref4 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref5 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref6 = mock(NotificationFilterPreference.class);

        // pref1 is not enabled, it will be discarded right away. Others are enabled.
        when(pref1.isEnabled()).thenReturn(false);
        when(pref2.isEnabled()).thenReturn(true);
        when(pref3.isEnabled()).thenReturn(true);
        when(pref4.isEnabled()).thenReturn(true);
        when(pref5.isEnabled()).thenReturn(true);
        when(pref6.isEnabled()).thenReturn(true);

        // pref2 has not the right filter name, it is discarded. Others have the right name.
        when(pref2.getFilterName()).thenReturn("Something");
        when(pref3.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref4.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref5.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref6.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);

        // All formats will be accepted
        when(pref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(pref4.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.ALERT, NotificationFormat.EMAIL)));
        when(pref5.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        when(pref6.getNotificationFormats()).thenReturn(null);

        // pref4 does not contain the right event type, it will be discarded.
        // pref5 does not contain any specific event type, and onlyGivenType is true so it doesn't match.
        when(pref3.getEventTypes()).thenReturn(Collections.singleton("mentions"));
        when(pref4.getEventTypes()).thenReturn(Collections.singleton("otherEventType"));
        when(pref5.getEventTypes()).thenReturn(Collections.emptySet());
        when(pref6.getEventTypes()).thenReturn(new HashSet<>(Arrays.asList("like", "mentions", "comment")));

        ScopeNotificationFilterPreferencesHierarchy expected = new ScopeNotificationFilterPreferencesHierarchy(
            Arrays.asList(
                new ScopeNotificationFilterPreference(pref3, this.entityReferenceResolver),
                new ScopeNotificationFilterPreference(pref6, this.entityReferenceResolver)
            ));
        assertEquals(expected, this.getter.getScopeFilterPreferences(Arrays.asList(
            pref1, pref2, pref3, pref4, pref5, pref6
        ), requestedEventType, requestedFormat, onlyGivenType, false));
    }

    @Test
    void getScopeFilterPreferencesCriteriaEventTypeNullOnlyGivenType()
    {
        NotificationFormat requestedFormat = null;
        String requestedEventType = null;
        boolean onlyGivenType = true;

        NotificationFilterPreference pref1 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref2 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref3 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref4 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref5 = mock(NotificationFilterPreference.class);
        NotificationFilterPreference pref6 = mock(NotificationFilterPreference.class);

        // pref1 is not enabled, it will be discarded right away. Others are enabled.
        when(pref1.isEnabled()).thenReturn(false);
        when(pref2.isEnabled()).thenReturn(true);
        when(pref3.isEnabled()).thenReturn(true);
        when(pref4.isEnabled()).thenReturn(true);
        when(pref5.isEnabled()).thenReturn(true);
        when(pref6.isEnabled()).thenReturn(true);

        // pref2 has not the right filter name, it is discarded. Others have the right name.
        when(pref2.getFilterName()).thenReturn("Something");
        when(pref3.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref4.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref5.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);
        when(pref6.getFilterName()).thenReturn(ScopeNotificationFilter.FILTER_NAME);

        // All formats will be accepted
        when(pref3.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.EMAIL));
        when(pref4.getNotificationFormats())
            .thenReturn(new HashSet<>(Arrays.asList(NotificationFormat.ALERT, NotificationFormat.EMAIL)));
        when(pref5.getNotificationFormats()).thenReturn(Collections.singleton(NotificationFormat.ALERT));
        when(pref6.getNotificationFormats()).thenReturn(null);

        // eventType is null and onlyGivenType is true so only pref5 matches.
        when(pref3.getEventTypes()).thenReturn(Collections.singleton("mentions"));
        when(pref4.getEventTypes()).thenReturn(Collections.singleton("otherEventType"));
        when(pref5.getEventTypes()).thenReturn(Collections.emptySet());
        when(pref6.getEventTypes()).thenReturn(new HashSet<>(Arrays.asList("like", "mentions", "comment")));

        ScopeNotificationFilterPreferencesHierarchy expected = new ScopeNotificationFilterPreferencesHierarchy(
            Collections.singletonList(
                new ScopeNotificationFilterPreference(pref5, this.entityReferenceResolver)
            ));
        assertEquals(expected, this.getter.getScopeFilterPreferences(Arrays.asList(
            pref1, pref2, pref3, pref4, pref5, pref6
        ), requestedEventType, requestedFormat, onlyGivenType, false));
    }
}
