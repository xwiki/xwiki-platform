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
package org.xwiki.notifications.sources.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.internal.EventStreamConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultParametrizedNotificationManager}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({SimilarityCalculator.class, EventSearcher.class})
class DefaultParametrizedNotificationManagerTest
{
    @InjectMockComponents
    private DefaultParametrizedNotificationManager defaultParametrizedNotificationManager;

    @MockComponent
    private EventStore eventStore;

    @MockComponent
    private EventQueryGenerator eventQueryGenerator;

    @MockComponent
    private EventStreamConfiguration configuration;

    @MockComponent
    private RecordableEventDescriptorHelper recordableEventDescriptorHelper;

    @BeforeEach
    public void setUp() throws Exception
    {
        NotificationPreference pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.isNotificationEnabled()).thenReturn(true);

        when(recordableEventDescriptorHelper.hasDescriptor(anyString(), any(DocumentReference.class))).thenReturn(true);
    }

    @Test
    void getEventsWhenNoPreferences() throws Exception
    {
        NotificationPreference pref1 = mock(NotificationPreference.class);
        when(pref1.getProperties())
            .thenReturn(Collections.singletonMap(NotificationPreferenceProperty.EVENT_TYPE, "create"));
        when(pref1.isNotificationEnabled()).thenReturn(false);

        // Test
        NotificationParameters parameters = new NotificationParameters();
        parameters.user = new DocumentReference("xwiki", "XWiki", "UserA");
        parameters.expectedCount = 2;
        parameters.preferences = Arrays.asList(pref1);
        List<CompositeEvent> results = this.defaultParametrizedNotificationManager.getEvents(parameters);

        // Verify
        assertEquals(0, results.size());
    }
}
