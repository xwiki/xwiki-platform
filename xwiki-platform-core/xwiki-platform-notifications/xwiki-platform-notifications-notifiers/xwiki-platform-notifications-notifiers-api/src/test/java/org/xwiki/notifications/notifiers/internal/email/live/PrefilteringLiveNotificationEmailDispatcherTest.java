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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link PrefilteringLiveNotificationEmailDispatcher}.
 * 
 * @version $Id$
 */
@ComponentTest
public class PrefilteringLiveNotificationEmailDispatcherTest
{
    @InjectMockComponents
    private PrefilteringLiveNotificationEmailDispatcher dispatcher;

    @MockComponent
    private PrefilteringLiveNotificationEmailSender sender;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @BeforeComponent
    void beforeComponent() throws ComponentLookupException
    {
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        when(this.componentManager.getInstance(ExecutionContextManager.class)).thenReturn(executionContextManager);
    }

    @Test
    void addEvent() throws IllegalAccessException, NotificationException
    {
        // Force a very short grace period so that the test does not take 1 minute
        FieldUtils.writeField(this.dispatcher, "grace", 100, true);

        //////
        // One event

        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");
        DefaultEvent event = new DefaultEvent();
        event.setId("id");
        event.setDate(new Date());

        this.dispatcher.addEvent(event, userReference);

        Map<DocumentReference, List<Event>> events = new HashMap<>();
        events.put(userReference, List.of(event));
        verifySendMailsCalled(events);

        //////
        // One event with two users

        DocumentReference user2Reference = new DocumentReference("wiki", "XWiki", "user2");
        DefaultEvent sameevent = new DefaultEvent();
        sameevent.setId("id");
        sameevent.setDate(new Date());
        event.setDate(new Date());

        this.dispatcher.addEvent(event, userReference);
        this.dispatcher.addEvent(sameevent, user2Reference);

        events = new HashMap<>();
        events.put(userReference, List.of(event));
        events.put(user2Reference, List.of(event));
        verifySendMailsCalled(events);

        //////
        // Two similar events

        DefaultEvent similarevent = new DefaultEvent();
        similarevent.setId("similar");
        similarevent.setDate(new Date());
        DefaultEvent otherevent = new DefaultEvent();
        otherevent.setId("other");
        otherevent.setDate(new Date());
        event.setDate(new Date());

        this.dispatcher.addEvent(event, userReference);
        this.dispatcher.addEvent(similarevent, userReference);
        this.dispatcher.addEvent(otherevent, userReference);

        // Wait until the sendMails() method is called with the right parameters or until it times out.
        // We need the wait because there's the grace period and processing the event can also take some time.
        events = new HashMap<>();
        events.put(userReference, List.of(event, similarevent, otherevent));
        verifySendMailsCalled(events);
    }

    private void verifySendMailsCalled(Map<DocumentReference, List<Event>> events)
    {
        // Wait until the sendMails() method is called with the right parameters or until it times out.
        // We need the wait because there's the grace period and processing the event can also take some time.
        verify(this.sender, timeout(5000).times(1)).sendMails(events);
    }
}
