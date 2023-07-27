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
package org.xwiki.notifications.notifiers.internal.email.grouping;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultNotificationEmailGroupingStrategy}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@ComponentTest
class DefaultNotificationEmailGroupingStrategyTest
{
    @InjectMockComponents
    private DefaultNotificationEmailGroupingStrategy groupingStrategy;

    @Test
    void groupEventsPerMail() throws NotificationException
    {
        CompositeEvent event1 = mock(CompositeEvent.class, "event1");
        CompositeEvent event2 = mock(CompositeEvent.class, "event2");
        CompositeEvent event3 = mock(CompositeEvent.class, "event3");
        CompositeEvent event4 = mock(CompositeEvent.class, "event4");

        List<CompositeEvent> input = List.of(event1, event2, event3, event4);
        assertEquals(List.of(input), this.groupingStrategy.groupEventsPerMail(input));
    }
}