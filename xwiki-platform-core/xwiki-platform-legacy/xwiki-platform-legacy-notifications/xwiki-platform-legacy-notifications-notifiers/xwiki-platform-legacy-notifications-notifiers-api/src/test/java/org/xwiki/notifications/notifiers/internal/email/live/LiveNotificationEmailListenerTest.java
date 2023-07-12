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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LiveNotificationEmailListener}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public class LiveNotificationEmailListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<LiveNotificationEmailListener> mocker =
            new MockitoComponentMockingRule<>(LiveNotificationEmailListener.class);

    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    private LiveNotificationEmailManager liveNotificationEmailManager;

    private NotificationConfiguration notificationConfiguration;

    private ComponentManager componentManager;

    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();

    @Before
    public void setUp() throws Exception
    {
        this.recordableEventDescriptorManager =
                this.mocker.registerMockComponent(RecordableEventDescriptorManager.class);

        this.liveNotificationEmailManager = this.mocker.registerMockComponent(LiveNotificationEmailManager.class);

        this.notificationConfiguration = this.mocker.registerMockComponent(NotificationConfiguration.class);

        this.componentManager = this.mocker.registerMockComponent(ComponentManager.class, "context");

        // Override the default output streams in order for the tests to pass even if the thread raised by the test
        // crashes and log its crash to console.
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @After
    public void cleanUpStreams() {
        System.setOut(null);
        System.setErr(null);
    }

    @Test
    public void testOnEvent() throws Exception
    {
        Event eventStreamEvent = mock(Event.class);
        EventStreamAddedEvent event = mock(EventStreamAddedEvent.class);

        RecordableEventDescriptor eventDescriptor = mock(RecordableEventDescriptor.class);
        when(eventDescriptor.getEventType()).thenReturn("eventType");
        when(this.recordableEventDescriptorManager.getRecordableEventDescriptors(true))
                .thenReturn(Arrays.asList(eventDescriptor));
        when(eventStreamEvent.getType()).thenReturn("eventType");

        when(this.notificationConfiguration.areEmailsEnabled()).thenReturn(true);
        when(this.notificationConfiguration.isEnabled()).thenReturn(true);

        this.mocker.getComponentUnderTest().onEvent(event, eventStreamEvent, null);

        verify(this.liveNotificationEmailManager, times(1)).addEvent(eventStreamEvent);
    }
}
