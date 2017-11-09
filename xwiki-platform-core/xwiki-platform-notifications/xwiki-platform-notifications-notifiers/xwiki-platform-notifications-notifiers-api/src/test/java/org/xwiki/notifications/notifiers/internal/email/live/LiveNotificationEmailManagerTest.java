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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveNotificationEmailManager}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public class LiveNotificationEmailManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<LiveNotificationEmailManager> mocker =
            new MockitoComponentMockingRule<>(LiveNotificationEmailManager.class);

    private NotificationConfiguration notificationConfiguration;

    private LiveNotificationEmailSender liveNotificationEmailSender;

    private SimilarityCalculator similarityCalculator;

    @Before
    public void setUp() throws Exception
    {
        this.notificationConfiguration = this.mocker.registerMockComponent(NotificationConfiguration.class);
        when(this.notificationConfiguration.liveNotificationsGraceTime()).thenReturn(1);

        this.liveNotificationEmailSender = this.mocker.registerMockComponent(LiveNotificationEmailSender.class);

        this.similarityCalculator = this.mocker.registerMockComponent(SimilarityCalculator.class);
    }

    @Test
    public void testNextExecutionWithNoEvents() throws Exception
    {
        assertEquals(null, this.mocker.getComponentUnderTest().getNextExecutionDate());
    }

    @Test
    public void testAddNewEvent() throws Exception
    {
        // No event is currently defined
        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        Event event3 = mock(Event.class);

        when(this.similarityCalculator.computeSimilarity(event1, event2))
                .thenReturn(SimilarityCalculator.NO_SIMILARITY);
        when(this.similarityCalculator.computeSimilarity(event2, event3))
                .thenReturn(SimilarityCalculator.SAME_DOCUMENT_AND_TYPE);

        this.mocker.getComponentUnderTest().initialize();
        this.mocker.getComponentUnderTest().addEvent(event1);
        this.mocker.getComponentUnderTest().addEvent(event2);
        this.mocker.getComponentUnderTest().addEvent(event3);

        // We assume that this test will take no more than 15 seconds to execute
        assertTrue(this.mocker.getComponentUnderTest().getNextExecutionDate().isAfter(
                DateTime.now().plusSeconds(45).getMillis()));
        assertTrue(this.mocker.getComponentUnderTest().getNextExecutionDate().isBefore(
                DateTime.now().plusMinutes(1).getMillis()));
    }
}
