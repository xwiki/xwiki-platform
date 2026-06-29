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

import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveNotificationEmailManager}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
@ComponentTest
class LiveNotificationEmailManagerTest
{
    @InjectMockComponents
    private LiveNotificationEmailManager manager;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    private LiveNotificationEmailSender liveNotificationEmailSender;

    @MockComponent
    private SimilarityCalculator similarityCalculator;

    @BeforeEach
    void setUp()
    {
        when(this.notificationConfiguration.liveNotificationsGraceTime()).thenReturn(1);
    }

    @Test
    void nextExecutionWithNoEvents()
    {
        assertNull(this.manager.getNextExecutionDate());
    }

    @Test
    void addNewEvent() throws Exception
    {
        // No event is currently defined
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);

        when(this.similarityCalculator.computeSimilarity(event1, event2))
                .thenReturn(SimilarityCalculator.NO_SIMILARITY);
        when(this.similarityCalculator.computeSimilarity(event2, event3))
                .thenReturn(SimilarityCalculator.SAME_DOCUMENT_AND_TYPE);

        this.manager.initialize();
        this.manager.addEvent(event1);
        this.manager.addEvent(event2);
        this.manager.addEvent(event3);

        // We assume that this test will take no more than 15 seconds to execute
        assertTrue(this.manager.getNextExecutionDate().isAfter(
                DateTime.now().plusSeconds(45).getMillis()));
        assertTrue(this.manager.getNextExecutionDate().isBefore(
                DateTime.now().plusMinutes(1).getMillis()));
    }
}
